@echo off
chcp 65001 > nul
echo ====================================================
echo  Connecting to my-postgres (PostgreSQL 16 Alpine)...
echo ====================================================
docker exec -it my-postgres psql -U postgres -d mydb
pause
