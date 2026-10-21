# Fullstack Microservices Mesh & MCP Hub v3.0 (Spring Boot 3 + FastAPI + Node.js/Redis + PostgreSQL 16 🔐)

[![CI Microservices Mesh Pipeline](https://github.com/IDAN2468D/my-postgres/actions/workflows/ci.yml/badge.svg)](https://github.com/IDAN2468D/my-postgres/actions/workflows/ci.yml)

ארכיטקטורת מיקרו-סרביסים ארגונית מלאה ב-**Docker**, המשלבת **Java 17 Spring Boot 3 (JWT Gateway)**, **Python FastAPI (AI Analytics & Currency)**, **Node.js 20 + Redis 7 (Cache & Event Bus)**, **PostgreSQL 16 Alpine**, ושרת **Model Context Protocol (MCP)** ייעודי המחובר ל-**Antigravity**.

---

## 🐳 מפת הקונטיינרים והמיקרו-סרביסים ב-Docker (`v3.0`)

| שירות | קונטיינר | טכנולוגיה | פורט וכתובת | תפקיד בארכיטקטורה |
| :--- | :--- | :--- | :--- | :--- |
| **webapp** | `my-webapp` | **Java 17 Spring Boot 3 + JWT** | `http://localhost:5001` | שרת ראשי, ממשק ניהול, אימות JWT ו-API Gateway |
| **analytics-service** | `my-analytics-service` | **Python 3.11 FastAPI** | `http://localhost:5002` | מיקרו-סרביס תובנות AI, חיזוי מלאי והמרת מטבעות (`₪ / $ / € / £`) |
| **events-service** | `my-events-service` | **Node.js 20 + Express** | `http://localhost:5003` | מיקרו-סרביס סנכרון קאש מהיר ב-Redis, מבחני ביצועים ואירועים בזמן אמת |
| **mcp-server** | `my-mcp-server` | **Node.js MCP (JSON-RPC 2.0)** | `http://localhost:5004` | שרת MCP ייעודי עם 8 כלים לשאילתות SQL וניהול המיקרו-סרביסים |
| **postgres** | `my-postgres` | **PostgreSQL 16 Alpine** | `localhost:5433` (`5432`) | מסד הנתונים הראשי (`mydb`) |
| **redis** | `my-redis` | **Redis 7 Alpine** | `localhost:6379` | שכבת In-Memory Cache ו-Event Bus |
| **adminer** | `my-adminer` | **Adminer Web GUI** | `http://localhost:8088` | ממשק ניהול מסדי נתונים בדפדפן |

---

## 🤖 חיבור ה-MCP (`my-postgres-hub`)

המערכת כוללת שרת **Model Context Protocol** מלא בשני מצבים:
1. **חיבור ישיר ל-Antigravity / Claude / Cursor (`stdio`)**:
   מוגדר אוטומטית בקובץ `C:\Users\kazam\.gemini\config\mcp_config.json` תחת השם `my-postgres-hub` ומריץ את `mcp-server/mcp-stdio.js`.
2. **מרכז שליטה חי בדפדפן (`MCP Live Hub` בפורט `5001` / `5004`)**:
   בלשונית **"🤖 מרכז שליטה MCP"** בממשק הראשי ניתן להריץ בזמן אמת את כל 8 כלי ה-MCP:
   - `postgres_query_readonly` — הרצת שאילתות `SELECT` בטוחות מול `PostgreSQL`.
   - `get_system_mesh_health` — בדיקת תקינות וזמני תגובה (`ms`) של כל 5 המיקרו-סרביסים.
   - `list_inventory_products` — שליפת קטלוג מוצרים וסינון מלאי נמוך.
   - `create_or_update_product` — הוספה ועדכון מוצרים ומלאי ב-`PostgreSQL` דרך `MCP`.
   - `get_ai_business_insights` — שליפת תובנות עסקיות והמרת מטבעות ממיקרו-סרביס ה-`Python FastAPI`.
   - `redis_cache_benchmark` — סנכרון קאש ומבחן מהירות `PostgreSQL` מול `Redis 7`.
   - `emit_realtime_alert` — שידור התראה חיה אל `Redis Event Bus`.
   - `get_audit_trail` — שליפת יומן האבטחה והפעילות (`audit_logs`).

---

## 🔌 נקודות קצה חדשות של ה-API Gateway (`Spring Boot :5001`)

- `GET /api/mesh/status` — בדיקת זמינות וזמני תגובה (`ms`) של כל המיקרו-סרביסים.
- `GET /api/mesh/analytics` — תובנות AI והמרת מטבעות ממיקרו-סרביס `FastAPI`.
- `GET /api/mesh/events` — שליפת פיד התראות ונתוני קאש ממיקרו-סרביס `Node.js + Redis`.
- `POST /api/mesh/cache-sync` — סנכרון קטלוג ל-`Redis` והשוואת מהירות שליפה מול `PostgreSQL`.
- `GET /api/mesh/mcp/tools` — רשימת כל כלי ה-`MCP` והסכמות שלהם.
- `POST /api/mesh/mcp/execute` — הרצת כלי `MCP` עם פרמטרים בפורמט `JSON`.

---

## 🧪 בדיקות יחידה ואינטגרציה (CI Pipeline)

המערכת כוללת פייפליין **GitHub Actions** מלא המריץ בדיקות אוטומטיות בכל `push` ו-`pull request`:

| שירות | טכנולוגיית בדיקה | פקודת הרצה מקומית |
| :--- | :--- | :--- |
| **Java Spring Boot** | `JUnit 5 + MockMvc + H2 In-Memory` | `mvn test -f server/pom.xml` |
| **Python FastAPI** | `pytest + unittest + FastAPI TestClient` | `python -m unittest discover -s analytics-service` |
| **Node.js Events** | `Node.js Test Runner (node:test)` | `npm test --prefix events-service` |
| **Node.js MCP** | `Node.js Test Runner (node:test)` | `npm test --prefix mcp-server` |
| **Docker Compose** | `Docker Compose Config Validator` | `docker compose config` |

