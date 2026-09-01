@echo off
chcp 65001 >nul
mysql -uroot -p123456 < "D:\Code\javacode\MallU\src\main\resources\sql\init.sql"
echo EXIT=%errorlevel%
