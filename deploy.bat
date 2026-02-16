@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

REM ============================================================
REM  部署启动脚本 - 服务器上使用 (Windows)
REM  用法:
REM    deploy.bat start    - 启动所有微服务
REM    deploy.bat stop     - 停止所有微服务
REM    deploy.bat restart  - 重启所有微服务
REM    deploy.bat status   - 查看运行状态
REM
REM  前提: 已安装 JDK 17+，MySQL/Redis/SRS 已启动
REM  修改中间件地址: 编辑 conf\ 下对应的 yml 文件
REM ============================================================

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

set "PID_DIR=%SCRIPT_DIR%.pids"
set "LOG_DIR=%SCRIPT_DIR%logs"
if not exist "%PID_DIR%" mkdir "%PID_DIR%"
if not exist "%LOG_DIR%" mkdir "%LOG_DIR%"

set "CMD=%~1"
if "%CMD%"=="" set "CMD=start"

if "%CMD%"=="start"   goto :do_start
if "%CMD%"=="stop"    goto :do_stop
if "%CMD%"=="restart" goto :do_restart
if "%CMD%"=="status"  goto :do_status
echo 用法: %~nx0 {start^|stop^|restart^|status}
exit /b 1

:do_start
    call :start_one base-service base-service-1.0.0-SNAPSHOT.jar 9001 base-service.yml
    call :start_one mall-service mall-service-1.0.0-SNAPSHOT.jar 9002 mall-service.yml
    call :start_one gateway gateway-1.0.0-SNAPSHOT.jar 9000 gateway.yml
    echo.
    echo [INFO] 所有服务启动完成
    echo   Gateway:       http://localhost:9000
    echo   base-service:  http://localhost:9001
    echo   mall-service:  http://localhost:9002
    exit /b 0

:do_stop
    for %%s in (gateway mall-service base-service) do (
        if exist "%PID_DIR%\%%s.pid" (
            set /p pid=<"%PID_DIR%\%%s.pid"
            taskkill /PID !pid! /F >nul 2>&1
            if not errorlevel 1 (
                echo [INFO] 已停止 %%s ^(PID !pid!^)
            )
            del /f "%PID_DIR%\%%s.pid" >nul 2>&1
        )
    )
    echo [INFO] 所有服务已停止
    exit /b 0

:do_restart
    call :do_stop
    timeout /t 2 /nobreak >nul
    call :do_start
    exit /b 0

:do_status
    echo.
    echo === 微服务状态 ===
    for %%e in ("base-service:9001" "mall-service:9002" "gateway:9000") do (
        for /f "tokens=1,2 delims=:" %%a in (%%e) do (
            set "svc_status=未运行"
            if exist "%PID_DIR%\%%a.pid" (
                set /p svc_pid=<"%PID_DIR%\%%a.pid"
                tasklist /FI "PID eq !svc_pid!" 2>nul | findstr "!svc_pid!" >nul 2>&1
                if not errorlevel 1 (
                    set "svc_status=运行中 (PID !svc_pid!, 端口 %%b)"
                )
            )
            echo   %%a: !svc_status!
        )
    )
    echo.
    exit /b 0

:start_one
    set "svc_name=%~1"
    set "svc_jar=%~2"
    set "svc_port=%~3"
    set "svc_conf=%~4"

    REM 检查是否已在运行
    if exist "%PID_DIR%\%svc_name%.pid" (
        set /p old_pid=<"%PID_DIR%\%svc_name%.pid"
        tasklist /FI "PID eq !old_pid!" 2>nul | findstr "!old_pid!" >nul 2>&1
        if not errorlevel 1 (
            echo [WARN] %svc_name% 已在运行 ^(PID !old_pid!^)，跳过
            exit /b 0
        )
    )

    echo [INFO] 启动 %svc_name% (端口 %svc_port%)...
    start "" /b javaw -jar "lib\%svc_jar%" --spring.config.location="file:conf\%svc_conf%" > "%LOG_DIR%\%svc_name%.log" 2>&1

    set "retries=0"
:wait_deploy_loop
    curl -s "http://localhost:%svc_port%" >nul 2>&1
    if not errorlevel 1 (
        for /f "tokens=5" %%p in ('netstat -ano 2^>nul ^| findstr "LISTENING" ^| findstr ":%svc_port% "') do (
            echo %%p> "%PID_DIR%\%svc_name%.pid"
        )
        echo [INFO] %svc_name% 启动成功
        exit /b 0
    )
    set /a retries+=1
    if !retries! geq 60 (
        echo [ERROR] %svc_name% 启动超时，查看日志: %LOG_DIR%\%svc_name%.log
        exit /b 1
    )
    timeout /t 2 /nobreak >nul
    goto :wait_deploy_loop
