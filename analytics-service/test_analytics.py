import os
import sys
from unittest.mock import MagicMock, patch

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

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

import time
import unittest
from fastapi.testclient import TestClient
from main import app, EXCHANGE_RATES, supervisor


class TestAnalyticsService(unittest.TestCase):

    def setUp(self):
        self.client = TestClient(app)
        supervisor.mcp._rpc_unreachable_until = time.time() + 3600
        supervisor.gemini._cloud_unreachable_until = time.time() + 3600

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
        self.assertTrue(data["multi_agents_enabled"])
        self.assertEqual(data["gemini_model"], "gemini-3.5-flash-lite")
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
        self.assertIn("gemini_engine", data)
        self.assertEqual(data["gemini_engine"]["model"], "gemini-3.5-flash-lite")
        self.assertIn("currencies", data)
        self.assertEqual(data["metrics"]["active_orders_count"], 1)
        self.assertEqual(data["metrics"]["low_stock_count"], 1)

    def test_supervisor_routing_logic(self):
        inv_agents = supervisor.select_agents_for_prompt("בדיקת מלאי קריטי והכנת הזמנות רכש")
        self.assertEqual(len(inv_agents), 1)
        self.assertEqual(inv_agents[0].name, "InventoryRestockAgent")

        order_agents = supervisor.select_agents_for_prompt("ביקורת אשראי לקוחות ואישור הזמנות")
        self.assertEqual(len(order_agents), 1)
        self.assertEqual(order_agents[0].name, "OrderFraudCreditAgent")

        bi_agents = supervisor.select_agents_for_prompt("ניתוח רווחיות, מלאי שוכב ותמחור מט\"ח")
        self.assertEqual(len(bi_agents), 1)
        self.assertEqual(bi_agents[0].name, "BiFinancialSqlAgent")

        sre_agents = supervisor.select_agents_for_prompt("סריקת בריאות, אבטחה וביצועי Redis")
        self.assertEqual(len(sre_agents), 1)
        self.assertEqual(sre_agents[0].name, "SreSecurityAgent")

        all_agents = supervisor.select_agents_for_prompt("סריקת מערכת מלאה")
        self.assertEqual(len(all_agents), 4)

    @patch("main.get_db_connection")
    def test_multi_agent_run_and_approval_endpoints(self, mock_get_db):
        mock_get_db.side_effect = Exception("Offline DB mode - use fallback")

        run_resp = self.client.post(
            "/api/analytics/agents/run",
            json={
                "prompt": "בדיקת מלאי קריטי והכנת הזמנות רכש",
                "auto_execute": False,
                "trigger_type": "USER_PROMPT"
            }
        )
        self.assertEqual(run_resp.status_code, 200)
        run_data = run_resp.json()
        self.assertEqual(run_data["status"], "success")
        self.assertIn("InventoryRestockAgent", run_data["active_agents"])
        self.assertIn("gemini_synthesis", run_data)
        self.assertEqual(run_data["gemini_synthesis"]["model"], "gemini-3.5-flash-lite")
        self.assertGreaterEqual(len(run_data["agent_steps"]), 1)
        self.assertGreaterEqual(len(run_data["pending_approvals"]), 1)

        runs_resp = self.client.get("/api/analytics/agents/runs")
        self.assertEqual(runs_resp.status_code, 200)
        self.assertEqual(runs_resp.json()["status"], "success")

        approve_resp = self.client.post(
            "/api/analytics/agents/approve/1",
            json={"action": "APPROVE", "reviewed_by": "CI Test Runner"}
        )
        self.assertEqual(approve_resp.status_code, 200)
        approve_data = approve_resp.json()
        self.assertEqual(approve_data["status"], "success")
        self.assertEqual(approve_data["decision"], "APPROVED")

    @patch("main.get_db_connection")
    def test_gemini_flash_lite_endpoints(self, mock_get_db):
        mock_get_db.side_effect = Exception("Offline DB mode - use fallback")

        status_resp = self.client.get("/api/analytics/gemini/status")
        self.assertEqual(status_resp.status_code, 200)
        self.assertEqual(status_resp.json()["gemini"]["model"], "gemini-3.5-flash-lite")

        chat_resp = self.client.post(
            "/api/analytics/gemini/chat",
            json={"prompt": "אילו מוצרים נמצאים במלאי נמוך ומה שווי המלאי?"}
        )
        self.assertEqual(chat_resp.status_code, 200)
        chat_data = chat_resp.json()
        self.assertEqual(chat_data["status"], "success")
        self.assertEqual(chat_data["model"], "gemini-3.5-flash-lite")
        self.assertIn("SELECT", chat_data["generated_sql"])

        assist_resp = self.client.post(
            "/api/analytics/gemini/product-assist",
            json={"product_name": "אוזניות גיימינג Sony Pro"}
        )
        self.assertEqual(assist_resp.status_code, 200)
        assist_data = assist_resp.json()
        self.assertEqual(assist_data["status"], "success")
        self.assertEqual(assist_data["model"], "gemini-3.5-flash-lite")
        self.assertEqual(assist_data["recommended_category"], "שמע")
        self.assertGreater(assist_data["recommended_price"], 0)


if __name__ == "__main__":
    unittest.main()
