@echo off
set PGPASSWORD=admin
"C:\Program Files\PostgreSQL\16\bin\psql.exe" -U postgres -d kompralo -f "C:\Users\USUARIO\Downloads\auth-backend\fix-fk.sql" > "C:\Users\USUARIO\Downloads\auth-backend\fk-result.txt" 2>&1
