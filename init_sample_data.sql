-- יצירת בסיס נתונים חדש בשם shop_db אם אינו קיים
SELECT 'CREATE DATABASE shop_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shop_db')\gexec

-- התחברות ל-mydb ויצירת טבלאות המערכת וטבלאות ה-Multi-Agents (v4.0)
\c mydb;

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    password VARCHAR(255),
    role VARCHAR(50) DEFAULT 'user',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(100) NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE CASCADE,
    product_id INT REFERENCES products(id) ON DELETE SET NULL,
    product_name VARCHAR(150),
    quantity INT DEFAULT 1,
    total_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'pending',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- וידוא תאימות עמודות למסדים קיימים
ALTER TABLE orders ADD COLUMN IF NOT EXISTS product_id INT REFERENCES products(id) ON DELETE SET NULL;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS product_name VARCHAR(150);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS quantity INT DEFAULT 1;

CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    details VARCHAR(500) NOT NULL,
    performed_by VARCHAR(150),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- טבלאות ניהול וזיכרון ל-Multi-Agents (v4.0)
CREATE TABLE IF NOT EXISTS agent_runs (
    id SERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    trigger_type VARCHAR(50) NOT NULL,
    user_goal TEXT NOT NULL,
    active_agents JSONB NOT NULL DEFAULT '[]'::jsonb,
    agent_steps JSONB NOT NULL DEFAULT '[]'::jsonb,
    final_summary TEXT,
    status VARCHAR(30) DEFAULT 'completed',
    execution_ms NUMERIC(10, 2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS agent_approvals (
    id SERIAL PRIMARY KEY,
    run_id INT REFERENCES agent_runs(id) ON DELETE CASCADE,
    agent_name VARCHAR(80) NOT NULL,
    mcp_tool_name VARCHAR(100) NOT NULL,
    proposed_arguments JSONB NOT NULL,
    reasoning_hebrew TEXT NOT NULL,
    status VARCHAR(30) DEFAULT 'PENDING',
    reviewed_by VARCHAR(150),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP
);

-- הזנת נתוני דוגמה
INSERT INTO users (name, email, role) VALUES
('עידן כהן', 'idan@example.com', 'admin'),
('נועה לוי', 'noa@example.com', 'user'),
('יוסי אברהם', 'yossi@example.com', 'user'),
('מיכל דוד', 'michal@example.com', 'editor')
ON CONFLICT (email) DO NOTHING;

INSERT INTO products (name, category, price, stock) VALUES
('מחשב נייד Lenovo ThinkPad', 'אלקטרוניקה', 4999.00, 15),
('עכבר אלחוטי ארגונומי', 'ציוד היקפי', 199.90, 50),
('מסך מחשב 4K 27 אינץ', 'מסכים', 1450.00, 20),
('מקלדת מכנית RGB', 'ציוד היקפי', 349.00, 35),
('אוזניות ביטול רעשים Sony', 'שמע', 899.00, 25),
('תחנת עגינה Thunderbolt 4 Pro', 'אלקטרוניקה', 1190.00, 3)
ON CONFLICT DO NOTHING;

INSERT INTO orders (user_id, product_id, product_name, quantity, total_amount, status) VALUES
(1, 1, 'מחשב נייד Lenovo ThinkPad', 1, 4999.00, 'completed'),
(2, 4, 'מקלדת מכנית RGB', 1, 349.00, 'shipped'),
(3, 3, 'מסך מחשב 4K 27 אינץ', 1, 1450.00, 'pending')
ON CONFLICT DO NOTHING;

INSERT INTO audit_logs (action, entity_type, entity_id, details, performed_by)
SELECT 'SYSTEM_SEED_V4', 'SYSTEM', 1, 'אתחול סכמת PostgreSQL 16 עם תמיכה במערכת Multi-Agent AI v4.0', 'System Architect'
WHERE NOT EXISTS (SELECT 1 FROM audit_logs);

-- הגדרת מסד נתונים שני shop_db להצלבות אשראי ולקוחות (Cross-DB Analytics)
\c shop_db;

CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    city VARCHAR(80),
    balance NUMERIC(12, 2) DEFAULT 0.00
);

INSERT INTO customers (full_name, email, city, balance) VALUES
('עידן כהן', 'idan@example.com', 'תל אביב', 15000.00),
('נועה לוי', 'noa@example.com', 'רמת גן', 4200.00),
('יוסי אברהם', 'yossi@example.com', 'הרצליה', 3850.00),
('מיכל דוד', 'michal@example.com', 'נתניה', 1900.00),
('ישראל ישראלי', 'israel@shop.co.il', 'תל אביב', 1250.50),
('דנה רון', 'dana@shop.co.il', 'חיפה', 340.00),
('איתי מזרחי', 'itay@shop.co.il', 'ירושלים', 0.00)
ON CONFLICT (email) DO NOTHING;
