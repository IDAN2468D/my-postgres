-- יצירת בסיס נתונים חדש בשם shop_db אם אינו קיים
SELECT 'CREATE DATABASE shop_db'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'shop_db')\gexec

-- התחברות ל-mydb ויצירת טבלאות לדוגמה
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
    total_amount NUMERIC(10, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'pending',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
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
('אוזניות ביטול רעשים Sony', 'שמע', 899.00, 25)
ON CONFLICT DO NOTHING;

INSERT INTO orders (user_id, total_amount, status) VALUES
(1, 4999.00, 'completed'),
(2, 548.90, 'shipped'),
(3, 1450.00, 'pending')
ON CONFLICT DO NOTHING;

-- העתקת המבנה גם ל-shop_db
\c shop_db;

CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(150) UNIQUE NOT NULL,
    city VARCHAR(80),
    balance NUMERIC(12, 2) DEFAULT 0.00
);

INSERT INTO customers (full_name, email, city, balance) VALUES
('ישראל ישראלי', 'israel@shop.co.il', 'תל אביב', 1250.50),
('דנה רון', 'dana@shop.co.il', 'חיפה', 340.00),
('איתי מזרחי', 'itay@shop.co.il', 'ירושלים', 0.00)
ON CONFLICT (email) DO NOTHING;
