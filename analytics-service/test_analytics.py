import sys
from unittest.mock import MagicMock, patch

# Provide mock for psycopg2 if running in environment without C-extensions
if "psycopg2" not in sys.modules:
    try:
        import psycopg2
    except ImportError:
        mock_pg = MagicMock()
        mock_extras = MagicMock()
        mock_extras.RealDictCursor = MagicMock()
        mock_pg.extras = mock_extras
        sys.modules["psycopg2"] = mock_pg
        sys.modules["psycopg2.extras"] = mock_extras

import unittest
from fastapi.testclient import TestClient
from main import app, EXCHANGE_RATES


class TestAnalyticsService(unittest.TestCase):

    def setUp(self):
        self.client = TestClient(app)

    def test_exchange_rates_structure(self):
        self.assertIn("ILS", EXCHANGE_RATES)
        self.assertIn("USD", EXCHANGE_RATES)
        self.assertIn("EUR", EXCHANGE_RATES)
        self.assertIn("GBP", EXCHANGE_RATES)

        self.assertEqual(EXCHANGE_RATES["ILS"]["rate"], 1.0)
        self.assertEqual(EXCHANGE_RATES["ILS"]["symbol"], "₪")
        self.assertGreater(EXCHANGE_RATES["USD"]["rate"], 0)
        self.assertEqual(EXCHANGE_RATES["USD"]["symbol"], "$")

    @patch("main.get_db_connection")
    def test_health_check_success(self, mock_get_db):
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_cur.fetchone.return_value = {"cnt": 12}
        mock_conn.cursor.return_value = mock_cur
        mock_get_db.return_value = mock_conn

        response = self.client.get("/health")
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["status"], "success")
        self.assertEqual(data["service"], "python-analytics-fastapi")
        self.assertTrue(data["database_connected"])
        self.assertEqual(data["products_indexed"], 12)

    @patch("main.get_db_connection")
    def test_health_check_db_failure(self, mock_get_db):
        mock_get_db.side_effect = Exception("Connection refused")

        response = self.client.get("/health")
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["status"], "error")
        self.assertFalse(data["database_connected"])
        self.assertIn("Connection refused", data["message"])

    @patch("main.get_db_connection")
    def test_convert_currency_endpoint(self, mock_get_db):
        mock_conn = MagicMock()
        mock_cur = MagicMock()
        mock_cur.fetchall.return_value = [
            {"id": 1, "name": "Test Product", "category": "General", "price": 100.0, "stock": 10}
        ]
        mock_conn.cursor.return_value = mock_cur
        mock_get_db.return_value = mock_conn

        response = self.client.get("/api/analytics/currency?currency=USD")
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["status"], "success")
        self.assertEqual(data["currency"], "USD")
        self.assertEqual(data["symbol"], "$")
        self.assertEqual(len(data["products"]), 1)
        self.assertEqual(data["products"][0]["price_ils"], 100.0)

    @patch("main.get_db_connection")
    def test_business_insights_endpoint(self, mock_get_db):
        mock_conn = MagicMock()
        mock_cur = MagicMock()

        mock_cur.fetchall.side_effect = [
            [
                {"id": 1, "name": "Product A", "category": "Tech", "price": 2000.0, "stock": 2},
                {"id": 2, "name": "Product B", "category": "Tech", "price": 500.0, "stock": 20}
            ],
            [
                {"id": 1, "user_id": 1, "total_amount": 4000.0, "status": "completed", "product_name": "Product A", "quantity": 2, "user_name": "Alice"}
            ]
        ]
        mock_conn.cursor.return_value = mock_cur
        mock_get_db.return_value = mock_conn

        response = self.client.get("/api/analytics/insights")
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["status"], "success")
        self.assertIn("metrics", data)
        self.assertIn("recommendations", data)
        self.assertIn("currencies", data)
        self.assertEqual(data["metrics"]["active_orders_count"], 1)
        self.assertEqual(data["metrics"]["low_stock_count"], 1)


if __name__ == "__main__":
    unittest.main()
