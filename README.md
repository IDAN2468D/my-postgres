# Fullstack Microservices Mesh, Multi-Agents AI & MCP Hub v4.0 (Spring Boot 3 + FastAPI + Node.js/Redis + PostgreSQL 16 🧠🔐)

[![CI Microservices Mesh Pipeline](https://github.com/IDAN2468D/my-postgres/actions/workflows/ci.yml/badge.svg)](https://github.com/IDAN2468D/my-postgres/actions/workflows/ci.yml)

ארכיטקטורת מיקרו-סרביסים ומערכת **Multi-Agent AI (`v4.0`)** ארגונית מלאה ב-**Docker**, המשלבת **Java 17 Spring Boot 3 (JWT Gateway)**, **Python FastAPI (Multi-Agent Supervisor + 4 Specialized Agents & Multi-Currency BI)**, **Node.js 20 + Redis 7 (Cache & Event Bus)**, **PostgreSQL 16 Alpine (`mydb` + `shop_db`)**, ושרת **Model Context Protocol (MCP)** ייעודי עם **12 כלים מוקשחים** המחובר ל-**Antigravity**.

---

## 🐳 מפת הקונטיינרים והמיקרו-סרביסים ב-Docker (`v4.0`)

| שירות | קונטיינר | טכנולוגיה | פורט וכתובת | תפקיד בארכיטקטורה |
| :--- | :--- | :--- | :--- | :--- |
| **webapp** | `my-webapp` | **Java 17 Spring Boot 3 + JWT** | `http://localhost:5001` | שרת ראשי, דשבורד 7 לשוניות, אימות JWT ו-API Gateway |
| **analytics-service** | `my-analytics-service` | **Python 3.11 FastAPI + Multi-Agents** | `http://localhost:5002` | מתזמר צוות סוכני AI (`Supervisor` + 4 מומחים), תובנות והמרת מטבעות (`₪ / $ / € / £`) |
| **events-service** | `my-events-service` | **Node.js 20 + Express** | `http://localhost:5003` | מיקרו-סרביס סנכרון קאש מהיר ב-Redis, מבחני ביצועים וטריגרים בזמן אמת לסוכנים |
| **mcp-server** | `my-mcp-server` | **Node.js MCP (JSON-RPC 2.0)** | `http://localhost:5004` | שרת MCP מוקשח עם **12 כלים** לשאילתות SQL, חציית מסדים וניהול הזמנות |
| **postgres** | `my-postgres` | **PostgreSQL 16 Alpine** | `localhost:5433` (`5432`) | מסד הנתונים הראשי (`mydb`) ומסד אשראי לקוחות (`shop_db`) |
| **redis** | `my-redis` | **Redis 7 Alpine** | `localhost:6380` (`6379`) | שכבת In-Memory Cache ו-Event Bus (`system:events`) |
| **adminer** | `my-adminer` | **Adminer Web GUI** | `http://localhost:8088` | ממשק ניהול מסדי נתונים בדפדפן |

---

## 🧠 צוות סוכני ה-AI (`v4.0 Multi-Agent Architecture`)

בלשונית **"🧠 צוות סוכני AI (Multi-Agents)"** בדשבורד הראשי פועל **`MultiAgentSupervisor`** המתזמר 4 סוכנים מומחים עם מנגנון **Human-in-the-Loop (HITL)**:
1. **`InventoryRestockAgent` (סוכן מלאי ורכש 📦)**: מאתר מוצרים במלאי קריטי (`<= 5`), מחשב כמות חידוש מומלצת (`+20` יח') ומייצר בקשות אישור עבור `create_or_update_product`.
2. **`OrderFraudCreditAgent` (סוכן בקרת הזמנות ואשראי חוצה-מסדים 🛡️)**: מצליב הזמנות `pending` מ-`mydb` מול יתרת הזכות של הלקוח במסד `shop_db` וממליץ אילו הזמנות לקדם ל-`shipped`.
3. **`BiFinancialSqlAgent` (סוכן BI פיננסי, מט"ח ו-Text-to-SQL 📊)**: מנתח רווחיות, מאתר מלאי עודף (`stock >= 30`) להצעות באנדל להגדלת ה-AOV ומתרגם לשערי `ILS / USD / EUR / GBP`.
4. **`SreSecurityAgent` (סוכן SRE, קאש ואבטחה ⚡)**: סורק זמני תגובה של כל המיקרו-סרביסים, מריץ מבחן האצה של `Redis 7` מול `PostgreSQL 16` ומבקר את יומן האבטחה (`audit_logs`).

---

## 🤖 חיבור ה-MCP (`my-postgres-hub` — 12 כלים פעילים)

1. `postgres_query_readonly` — הרצת שאילתות `SELECT` מוקשחות בטרנזקציית `READ ONLY` עם `statement_timeout = 3000ms`.
2. `get_system_mesh_health` — בדיקת תקינות וזמני תגובה (`ms`) של כל המיקרו-סרביסים.
3. `list_inventory_products` — שליפת קטלוג מוצרים וסינון מלאי נמוך.
4. `create_or_update_product` — הוספה ועדכון מוצרים ומלאי ב-`PostgreSQL` דרך `MCP`.
5. `get_ai_business_insights` — שליפת תובנות עסקיות והמרת מטבעות ממיקרו-סרביס ה-`Python FastAPI`.
6. `redis_cache_benchmark` — סנכרון קאש ומבחן מהירות `PostgreSQL` מול `Redis 7`.
7. `emit_realtime_alert` — שידור התראה חיה אל `Redis Event Bus`.
8. `get_audit_trail` — שליפת יומן האבטחה והפעילות (`audit_logs`).
9. `list_or_filter_orders` — שליפת הזמנות בצירוף פרטי הלקוח (`LEFT JOIN users`) וסינון לפי סטטוס.
10. `update_order_workflow_status` — עדכון סטטוס הזמנה עם החזרת/ניכוי מלאי אוטומטי ותיעוד ב-`audit_logs`.
11. `cross_db_customer_profile` — הצלבת פרופיל לקוחות ויתרות אשראי בין `mydb.users` לבין `shop_db.customers`.
12. `list_pending_agent_approvals` — שליפת הצעות סוכנים הממתינות לאישור אנושי (`status = 'PENDING'`).

---

## 🔌 נקודות קצה של ה-API Gateway (`Spring Boot :5001`)

- `GET /api/mesh/status` — בדיקת זמינות וזמני תגובה (`ms`) של כל המיקרו-סרביסים.
- `GET /api/mesh/analytics` — תובנות AI והמרת מטבעות ממיקרו-סרביס `FastAPI`.
- `GET /api/mesh/events` — שליפת פיד התראות, מדדי קאש ומונה טריגרים של סוכנים (`agent_triggers`).
- `POST /api/mesh/cache-sync` — סנכרון קטלוג ל-`Redis` והשוואת מהירות שליפה מול `PostgreSQL`.
- `GET /api/mesh/mcp/tools` — רשימת כל 12 כלי ה-`MCP` והסכמות שלהם.
- `POST /api/mesh/mcp/execute` — הרצת כלי `MCP` עם פרמטרים בפורמט `JSON`.
- `POST /api/mesh/agents/run` — הרצת משימת צוות סוכני AI (`MultiAgentSupervisor`).
- `GET /api/mesh/agents/runs` — היסטוריית ריצות הסוכנים (`agent_runs`) והצעות הממתינות לאישור (`agent_approvals`).
- `POST /api/mesh/agents/approve/{id}` — אישור (`APPROVE`) או דחייה (`REJECT`) של הצעת סוכן וביצוע ב-MCP.

---

## 🧪 בדיקות יחידה ואינטגרציה (CI Pipeline)

המערכת כוללת פייפליין **GitHub Actions** מלא המריץ בדיקות אוטומטיות בכל `push` ו-`pull request`:

| שירות | טכנולוגיית בדיקה | פקודת הרצה מקומית |
| :--- | :--- | :--- |
| **Java Spring Boot** | `JUnit 5 + MockMvc + H2 In-Memory` | `mvn test -f server/pom.xml` |
| **Python FastAPI & Agents** | `pytest + unittest + FastAPI TestClient` | `python -m unittest discover -s analytics-service` |
| **Node.js Events** | `Node.js Test Runner (node:test)` | `npm test --prefix events-service` |
| **Node.js MCP (12 Tools)** | `Node.js Test Runner (node:test)` | `npm test --prefix mcp-server` |
| **Docker Compose** | `Docker Compose Config Validator` | `docker compose config` |
