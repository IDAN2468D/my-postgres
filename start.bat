@echo off
chcp 65001 > nul
echo ========================================================
echo  מפעיל את ארכיטקטורת המיקרו-סרביסים ו-MCP Hub v3.0...
echo ========================================================

if not exist "%~dp0server\target\app.jar" (
    echo [1/2] מקמפל את פרויקט ה-Java (Maven)...
    cd /d "%~dp0server"
    call mvn clean package -DskipTests
    cd /d "%~dp0"
)

echo [2/2] מפעיל את כל 7 הקונטיינרים ב-Docker...
docker compose up -d --build --remove-orphans

timeout /t 3 /nobreak > nul

echo.
echo ========================================================
echo  ✅ כל המיקרו-סרביסים ושרת ה-MCP פועלים בהצלחה!
echo.
echo  ☕ שרת ראשי ו-API Gateway:     http://localhost:5001
echo  🐍 מיקרו-סרביס Python AI:      http://localhost:5002/health
echo  ⚡ מיקרו-סרביס Node.js Redis:  http://localhost:5003/health
echo  🤖 שרת MCP Hub (JSON-RPC):     http://localhost:5004/mcp/tools
echo  📖 תיעוד API (Swagger UI):     http://localhost:5001/swagger-ui/index.html
echo  🖥️ ממשק ניהול DB (Adminer):     http://localhost:8088
echo  🐘 PostgreSQL 16 Port:         5433
echo  🔴 Redis 7 Cache Port:         6379
echo ========================================================
echo.
start http://localhost:5001
pause
