@echo off
chcp 65001 >nul
REM =====================================================
REM MallU 秒杀压测一键脚本（Windows）
REM 用法: run_stress.bat [并发数] [商品ID]
REM 默认: 500 并发, 商品ID=1
REM =====================================================
setlocal

set JMETER=D:\Software\apache-jmeter-5.6.3\bin\jmeter.bat
set PYTHON=python
set HOST=http://localhost:8080

set THREADS=%1
if "%THREADS%"=="" set THREADS=500
set GOODS_ID=%2
if "%GOODS_ID%"=="" set GOODS_ID=1

echo ============================================
echo [1/3] 准备压测用户 (%THREADS% 个)
echo ============================================
%PYTHON% prepare_users.py %THREADS%
if errorlevel 1 goto :error

echo.
echo ============================================
echo [2/3] 重置秒杀库存 (商品ID=%GOODS_ID%)
echo 请确认 MySQL/Redis 已就绪
echo ============================================
mysql -uroot -p123456 mallu -e "UPDATE seckill_goods SET stock = total_stock WHERE id = %GOODS_ID%;"
redis-cli -n 0 del "seckill:stock:%GOODS_ID%" "seckill:users:%GOODS_ID%"

echo.
echo ============================================
echo [3/3] 启动 JMeter 压测 (并发=%THREADS%)
echo ============================================
%JMETER% -n -t seckill_stress.jmx -Jthreads=%THREADS% -JseckillGoodsId=%GOODS_ID% -Jhost=localhost -Jport=8080 -l result.jtl -e -o report

echo.
echo ============================================
echo 压测完成! 结果:
echo   - 原始数据: result.jtl
echo   - HTML报告: report/index.html
echo ============================================
echo 验证超卖:
mysql -uroot -p123456 mallu -e "SELECT id, stock, total_stock FROM seckill_goods WHERE id = %GOODS_ID%; SELECT COUNT(*) AS seckill_orders FROM seckill_order WHERE seckill_goods_id = %GOODS_ID%;"
goto :end

:error
echo 压测失败，请检查 JMeter / MallU / MySQL / Redis 是否就绪
:end
endlocal
