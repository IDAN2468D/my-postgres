@echo off
chcp 65001 > nul
echo עוצר את כל שירותי ה-Docker...
docker compose stop
echo.
echo כל השירותים נעצרו בהצלחה.
pause
