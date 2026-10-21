@echo off
chcp 65001 > nul
echo מפעיל את שרת ה-Java מקומית (מחובר ל-PostgreSQL בפורט 5433)...
mvn spring-boot:run
pause
