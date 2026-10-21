import os
import time
from datetime import datetime, timezone
from typing import Optional
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import psycopg2
from psycopg2.extras import RealDictCursor

from multi_agent_orchestrator import MultiAgentSupervisor

app = FastAPI(
    title="AI Analytics, Currency & Multi-Agents Microservice",
    version="4.0.0",
    description="Python FastAPI Microservice for Multi-Agent AI Orchestration, Business Intelligence, Inventory Forecasting & Multi-Currency Conversion"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

START_TIME = time.time()

PG_HOST = os.getenv("PGHOST", "postgres")
PG_PORT = os.getenv("PGPORT", "5432")
PG_DB = os.getenv("PGDATABASE", "mydb")
PG_USER = os.getenv("PGUSER", "postgres")
PG_PASS = os.getenv("PGPASSWORD", "password")

# Reference Exchange Rates (Base: ILS ₪)
EXCHANGE_RATES = {
    "ILS": {"symbol": "₪", "rate": 1.0, "name": "שקל חדש (ILS)"},
    "USD": {"symbol": "$", "rate": 0.272, "name": "דולר אמריקאי (USD)"},
    "EUR": {"symbol": "€", "rate": 0.251, "name": "אירו (EUR)"},
    "GBP": {"symbol": "£", "rate": 0.214, "name": "לירה שטרלינג (GBP)"},
}


def get_db_connection():
    return psycopg2.connect(
        host=PG_HOST,
        port=PG_PORT,
        dbname=PG_DB,
        user=PG_USER,
        password=PG_PASS,
        cursor_factory=RealDictCursor
    )


supervisor = MultiAgentSupervisor(
    db_conn_factory=lambda: get_db_connection(),
    exchange_rates=EXCHANGE_RATES
)


class AgentMissionRequest(BaseModel):
    prompt: Optional[str] = "סריקת מערכת מלאה של כל הסוכנים"
    auto_execute: Optional[bool] = False
    trigger_type: Optional[str] = "USER_PROMPT"


class AgentApprovalReviewRequest(BaseModel):
    action: Optional[str] = "APPROVE"
    reviewed_by: Optional[str] = "מנהל מערכת (Human-in-the-Loop)"


@app.get("/health")
def health_check():
    t0 = time.time()
    try:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute("SELECT COUNT(*) AS cnt FROM products;")
        row = cur.fetchone()
        cur.close()
        conn.close()
        latency_ms = round((time.time() - t0) * 1000, 2)
        return {
            "status": "success",
            "service": "python-analytics-fastapi",
            "version": "4.0.0",
            "port": int(os.getenv("PORT", "5002")),
            "database_connected": True,
            "products_indexed": row["cnt"] if row else 0,
            "multi_agents_enabled": True,
            "latency_ms": latency_ms,
            "uptime_seconds": round(time.time() - START_TIME, 1),
            "timestamp": datetime.now(timezone.utc).isoformat()
        }
    except Exception as e:
        return {
            "status": "error",
            "service": "python-analytics-fastapi",
            "database_connected": False,
            "message": str(e)
        }


@app.get("/api/analytics/insights")
def get_business_insights():
    try:
        conn = get_db_connection()
        cur = conn.cursor()

        cur.execute("SELECT id, name, category, price, stock FROM products ORDER BY price * stock DESC;")
        products = cur.fetchall()

        cur.execute("""
            SELECT o.id, o.user_id, o.total_amount, o.status, o.product_name, o.quantity, u.name AS user_name
            FROM orders o
            LEFT JOIN users u ON o.user_id = u.id
            ORDER BY o.id DESC;
        """)
        orders = cur.fetchall()

        cur.close()
        conn.close()

        total_inventory_ils = sum(float(p["price"]) * int(p["stock"]) for p in products)
        active_orders = [o for o in orders if (o.get("status") or "").lower() != "cancelled"]
        total_revenue_ils = sum(float(o["total_amount"]) for o in active_orders)
        avg_order_ils = (total_revenue_ils / len(active_orders)) if active_orders else 0.0

        # Generate Smart AI Recommendations in Hebrew
        recommendations = []
        low_stock_items = [p for p in products if int(p["stock"]) <= 5]
        high_value_items = [p for p in products if float(p["price"]) * int(p["stock"]) > 15000]

        if low_stock_items:
            names = ", ".join(f"{p['name']} ({p['stock']} יח')" for p in low_stock_items[:3])
            recommendations.append({
                "severity": "warning",
                "icon": "⚠️",
                "title": "התראת חידוש מלאי דחופה",
                "description": f"זוהו {len(low_stock_items)} מוצרים במלאי קריטי: {names}. מומלץ להזמין סחורה מספקים."
            })
        else:
            recommendations.append({
                "severity": "success",
                "icon": "✅",
                "title": "רמות מלאי אופטימליות",
                "description": "כל המוצרים בקטלוג נמצאים מעל רף המלאי המינימלי (מעל 5 יחידות)."
            })

        if high_value_items:
            top_p = high_value_items[0]
            top_val = float(top_p["price"]) * int(top_p["stock"])
            recommendations.append({
                "severity": "info",
                "icon": "💎",
                "title": "מוצר עוגן מוביל בשווי מלאי",
                "description": f"המוצר '{top_p['name']}' מהווה נכס מלאי של ₪{top_val:,.2f} ({top_p['stock']} יח' במלאי)."
            })

        recommendations.append({
            "severity": "info",
            "icon": "📈",
            "title": "ניתוח סל קנייה ממוצע (AOV)",
            "description": f"שווי הזמנה ממוצע עומד על ₪{avg_order_ils:,.2f} מתוך {len(active_orders)} הזמנות פעילות."
        })

        # Top customers ranking
        customer_spend = {}
        for o in active_orders:
            uname = o.get("user_name") or f"לקוח #{o.get('user_id') or 'אורח'}"
            customer_spend[uname] = customer_spend.get(uname, 0.0) + float(o["total_amount"])

        top_customers = [
            {"name": k, "total_spent_ils": round(v, 2)}
            for k, v in sorted(customer_spend.items(), key=lambda x: x[1], reverse=True)[:5]
        ]

        # Multi-currency conversions
        currencies_summary = {}
        for code, meta in EXCHANGE_RATES.items():
            rate = meta["rate"]
            currencies_summary[code] = {
                "code": code,
                "symbol": meta["symbol"],
                "name": meta["name"],
                "rate_from_ils": rate,
                "total_revenue": round(total_revenue_ils * rate, 2),
                "inventory_value": round(total_inventory_ils * rate, 2),
                "avg_order_value": round(avg_order_ils * rate, 2)
            }

        return {
            "status": "success",
            "service": "python-analytics-fastapi",
            "metrics": {
                "total_revenue_ils": round(total_revenue_ils, 2),
                "total_inventory_ils": round(total_inventory_ils, 2),
                "avg_order_value_ils": round(avg_order_ils, 2),
                "active_orders_count": len(active_orders),
                "low_stock_count": len(low_stock_items)
            },
            "recommendations": recommendations,
            "top_customers": top_customers,
            "currencies": currencies_summary
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}


@app.get("/api/analytics/currency")
def convert_catalog_currency(currency: str = "USD"):
    code = currency.upper()
    if code not in EXCHANGE_RATES:
        code = "USD"
    meta = EXCHANGE_RATES[code]
    rate = meta["rate"]

    try:
        conn = get_db_connection()
        cur = conn.cursor()
        cur.execute("SELECT id, name, category, price, stock FROM products ORDER BY id DESC;")
        products = cur.fetchall()
        cur.close()
        conn.close()

        converted = []
        for p in products:
            price_ils = float(p["price"])
            converted.append({
                "id": p["id"],
                "name": p["name"],
                "category": p["category"],
                "stock": p["stock"],
                "price_ils": round(price_ils, 2),
                "price_converted": round(price_ils * rate, 2),
                "formatted": f"{meta['symbol']}{price_ils * rate:,.2f}"
            })

        return {
            "status": "success",
            "currency": code,
            "symbol": meta["symbol"],
            "rate": rate,
            "products": converted
        }
    except Exception as e:
        return {"status": "error", "message": str(e)}


@app.post("/api/analytics/agents/run")
def run_multi_agent_mission(req: AgentMissionRequest):
    return supervisor.run_mission(
        prompt=req.prompt or "סריקת מערכת מלאה של כל הסוכנים",
        trigger_type=req.trigger_type or "USER_PROMPT",
        auto_execute=bool(req.auto_execute)
    )


@app.get("/api/analytics/agents/runs")
def get_multi_agent_runs(limit: int = 10):
    return supervisor.get_runs_and_approvals(limit=limit)


@app.post("/api/analytics/agents/approve/{approval_id}")
def approve_or_reject_agent_action(approval_id: int, req: AgentApprovalReviewRequest):
    return supervisor.review_approval(
        approval_id=approval_id,
        action=req.action or "APPROVE",
        reviewed_by=req.reviewed_by or "מנהל מערכת (Human-in-the-Loop)"
    )
