@echo off
chcp 65001 >nul 2>&1
setlocal enabledelayedexpansion

REM ============================================================
REM  一键启动直播电商平台 (Windows)
REM  用法: start.bat [命令]
REM    start.bat          - 启动中间件 + 编译 + 启动所有服务
REM    start.bat infra    - 只启动中间件 (MySQL/Redis/SRS)
REM    start.bat build    - 只编译项目
REM    start.bat services - 只启动 3 个微服务 (需中间件已运行)
REM    start.bat stop     - 停止所有服务和中间件
REM    start.bat status   - 查看运行状态
REM    start.bat package  - 打包成可部署的 zip
REM ============================================================

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

set "PID_DIR=%SCRIPT_DIR%.pids"
if not exist "%PID_DIR%" mkdir "%PID_DIR%"

set "CMD=%~1"
if "%CMD%"=="" set "CMD=all"

if "%CMD%"=="infra"    goto :cmd_infra
if "%CMD%"=="build"    goto :cmd_build
if "%CMD%"=="services" goto :cmd_services
if "%CMD%"=="stop"     goto :cmd_stop
if "%CMD%"=="status"   goto :cmd_status
if "%CMD%"=="package"  goto :cmd_package
if "%CMD%"=="all"      goto :cmd_all

echo 用法: %~nx0 {all^|infra^|build^|services^|stop^|status^|package}
exit /b 1

REM ============================================================
:cmd_all
    call :check_deps
    if errorlevel 1 exit /b 1
    call :start_infra
    if errorlevel 1 exit /b 1
    call :build_project
    if errorlevel 1 exit /b 1
    call :start_services
    exit /b 0

:cmd_infra
    call :check_deps
    if errorlevel 1 exit /b 1
    call :start_infra
    exit /b 0

:cmd_build
    call :build_project
    exit /b 0

:cmd_services
    call :start_services
    exit /b 0

:cmd_stop
    call :stop_all
    exit /b 0

:cmd_status
    call :show_status
    exit /b 0

:cmd_package
    call :do_package
    exit /b 0

REM ============================================================
REM  检查依赖
REM ============================================================
:check_deps
    where docker >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] 未找到 docker，请先安装 Docker Desktop
        exit /b 1
    )
    where java >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] 未找到 java，请先安装 JDK 17+
        exit /b 1
    )
    docker info >nul 2>&1
    if errorlevel 1 (
        echo [ERROR] Docker 未运行，请先启动 Docker Desktop
        exit /b 1
    )
    for /f "tokens=3" %%v in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set "JAVA_VER=%%~v"
    )
    for /f "tokens=1 delims=." %%m in ("!JAVA_VER!") do (
        if %%m lss 17 (
            echo [ERROR] 需要 JDK 17+，当前: !JAVA_VER!
            exit /b 1
        )
    )
    echo [INFO] 依赖检查通过
    exit /b 0

REM ============================================================
REM  启动中间件
REM ============================================================
:start_infra
    echo [STEP] 启动中间件 (MySQL + Redis + SRS)...
    docker compose up -d
    if errorlevel 1 (
        echo [ERROR] Docker Compose 启动失败
        exit /b 1
    )

    echo [INFO] 等待 MySQL 就绪...
    set "retries=0"
:wait_mysql
    docker exec live-commerce-mysql mysqladmin ping -h localhost -u root -proot123 --silent >nul 2>&1
    if not errorlevel 1 goto :mysql_ready
    set /a retries+=1
    if !retries! geq 30 (
        echo [ERROR] MySQL 启动超时
        exit /b 1
    )
    timeout /t 2 /nobreak >nul
    goto :wait_mysql
:mysql_ready
    echo [INFO] MySQL 就绪

    echo [INFO] 等待 Redis 就绪...
    set "retries=0"
:wait_redis
    for /f %%r in ('docker exec live-commerce-redis redis-cli ping 2^>nul') do (
        if "%%r"=="PONG" goto :redis_ready
    )
    set /a retries+=1
    if !retries! geq 15 (
        echo [ERROR] Redis 启动超时
        exit /b 1
    )
    timeout /t 1 /nobreak >nul
    goto :wait_redis
:redis_ready
    echo [INFO] Redis 就绪

    echo.
    echo   MySQL:  localhost:3306 (root/root123)
    echo   Redis:  localhost:6379
    echo   SRS:    RTMP=localhost:1935  HTTP-FLV=localhost:8080  API=localhost:1985
    echo.
    echo [INFO] 中间件全部启动完成
    exit /b 0

REM ============================================================
REM  编译项目
REM ============================================================
:build_project
    echo [STEP] 编译项目...
    call mvnw.cmd clean package -DskipTests -q
    if errorlevel 1 (
        echo [ERROR] 编译失败
        exit /b 1
    )
    echo [INFO] 编译完成
    exit /b 0

REM ============================================================
REM  启动微服务
REM ============================================================
:start_services
    echo [STEP] 启动微服务...

    call :start_service base-service base-service 9001
    call :start_service mall-service mall-service 9002

    call :wait_for_service base-service 9001
    call :wait_for_service mall-service 9002

    call :start_service gateway gateway 9000
    call :wait_for_service gateway 9000

    echo.
    echo [INFO] 所有服务启动完成!
    echo.
    echo   ==========================================
    echo   Gateway:       http://localhost:9000
    echo   base-service:  http://localhost:9001
    echo   mall-service:  http://localhost:9002
    echo   ==========================================
    echo   Swagger UI:
    echo     base-service: http://localhost:9001/swagger-ui.html
    echo     mall-service: http://localhost:9002/swagger-ui.html
    echo   ==========================================
    echo   推流地址: rtmp://localhost:1935/live/{streamKey}
    echo   拉流地址: http://localhost:8080/live/{streamKey}.flv
    echo   ==========================================
    echo.
    echo   停止服务: start.bat stop
    echo   查看状态: start.bat status
    echo   查看日志: type .pids\base-service.log
    echo.
    exit /b 0

:start_service
    set "svc_name=%~1"
    set "svc_module=%~2"
    set "svc_port=%~3"

    REM 检查端口是否被占用
    netstat -ano 2>nul | findstr "LISTENING" | findstr ":%svc_port% " >nul 2>&1
    if not errorlevel 1 (
        echo [WARN] %svc_name% port %svc_port% already running, skip
        exit /b 0
    )

    echo [INFO] starting %svc_name% on port %svc_port%...
    start "" /b javaw -jar "%svc_module%\target\%svc_module%-1.0.0-SNAPSHOT.jar" > "%PID_DIR%\%svc_name%.log" 2>&1

    REM 记录 PID（通过端口查找，稍后在 wait 中写入）
    exit /b 0

:wait_for_service
    set "svc_name=%~1"
    set "svc_port=%~2"
    set "retries=0"
:wait_svc_loop
    curl -s "http://localhost:%svc_port%" >nul 2>&1
    if not errorlevel 1 (
        REM 获取并记录 PID
        for /f "tokens=5" %%p in ('netstat -ano 2^>nul ^| findstr "LISTENING" ^| findstr ":%svc_port% "') do (
            echo %%p> "%PID_DIR%\%svc_name%.pid"
        )
        echo [INFO] %svc_name% 启动成功
        exit /b 0
    )
    set /a retries+=1
    if !retries! geq 60 (
        echo [ERROR] %svc_name% 启动超时，查看日志: %PID_DIR%\%svc_name%.log
        exit /b 1
    )
    timeout /t 2 /nobreak >nul
    goto :wait_svc_loop

REM ============================================================
REM  停止
REM ============================================================
:stop_all
    call :stop_services
    call :stop_infra
    echo [INFO] 全部已停止
    exit /b 0

:stop_services
    echo [STEP] 停止微服务...
    for %%s in (gateway mall-service base-service) do (
        if exist "%PID_DIR%\%%s.pid" (
            set /p pid=<"%PID_DIR%\%%s.pid"
            taskkill /PID !pid! /F >nul 2>&1
            if not errorlevel 1 (
                echo [INFO] 已停止 %%s (PID !pid!)
            )
            del /f "%PID_DIR%\%%s.pid" >nul 2>&1
        )
    )
    exit /b 0

:stop_infra
    echo [STEP] 停止中间件...
    docker compose down
    echo [INFO] 中间件已停止
    exit /b 0

REM ============================================================
REM  打包部署
REM ============================================================
:do_package
    call :build_project
    if errorlevel 1 exit /b 1

    set "DIST_DIR=%SCRIPT_DIR%dist"
    set "DIST_NAME=live-commerce"

    echo [STEP] 打包部署包...

    if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
    mkdir "%DIST_DIR%\%DIST_NAME%\lib"
    mkdir "%DIST_DIR%\%DIST_NAME%\conf"
    mkdir "%DIST_DIR%\%DIST_NAME%\sql"
    mkdir "%DIST_DIR%\%DIST_NAME%\srs"
    mkdir "%DIST_DIR%\%DIST_NAME%\logs"

    REM 复制 jar 包
    copy "base-service\target\base-service-1.0.0-SNAPSHOT.jar" "%DIST_DIR%\%DIST_NAME%\lib\" >nul
    copy "mall-service\target\mall-service-1.0.0-SNAPSHOT.jar" "%DIST_DIR%\%DIST_NAME%\lib\" >nul
    copy "gateway\target\gateway-1.0.0-SNAPSHOT.jar" "%DIST_DIR%\%DIST_NAME%\lib\" >nul
    echo [INFO] JAR 包已复制

    REM 复制配置文件（部署时可独立修改）
    copy "base-service\src\main\resources\application.yml" "%DIST_DIR%\%DIST_NAME%\conf\base-service.yml" >nul
    copy "mall-service\src\main\resources\application.yml" "%DIST_DIR%\%DIST_NAME%\conf\mall-service.yml" >nul
    copy "gateway\src\main\resources\application.yml" "%DIST_DIR%\%DIST_NAME%\conf\gateway.yml" >nul
    echo [INFO] 配置文件已复制

    REM 复制中间件相关
    copy "docker-compose.yml" "%DIST_DIR%\%DIST_NAME%\" >nul
    copy "sql\init.sql" "%DIST_DIR%\%DIST_NAME%\sql\" >nul
    copy "srs\srs.conf" "%DIST_DIR%\%DIST_NAME%\srs\" >nul

    REM 复制前端页面
    xcopy "frontend" "%DIST_DIR%\%DIST_NAME%\frontend\" /E /I /Y >nul

    REM 复制部署脚本
    copy "deploy.sh" "%DIST_DIR%\%DIST_NAME%\" >nul
    copy "deploy.bat" "%DIST_DIR%\%DIST_NAME%\" >nul

    REM 打 zip
    cd "%DIST_DIR%"
    tar -acf "%DIST_NAME%.zip" "%DIST_NAME%"
    cd "%SCRIPT_DIR%"

    echo.
    echo [INFO] 打包完成!
    echo.
    echo   部署包: dist\%DIST_NAME%.zip
    echo.
    echo   部署步骤:
    echo     1. 上传到服务器并解压
    echo     2. 修改配置: 编辑 conf\ 下的 yml 文件 (MySQL/Redis 地址等)
    echo     3. 启动中间件: docker compose up -d (或使用已有的中间件)
    echo     4. 导入种子数据: mysql -uroot -p ^< sql\init.sql
    echo     5. 启动服务: deploy.bat start (Windows) / ./deploy.sh start (Linux)
    echo     6. 停止服务: deploy.bat stop / ./deploy.sh stop
    echo.
    exit /b 0

REM ============================================================
REM  状态
REM ============================================================
:show_status
    echo.
    echo === 中间件状态 ===
    docker compose ps 2>nul || echo   Docker Compose 未运行
    echo.
    echo === 微服务状态 ===
    for %%e in ("base-service:9001" "mall-service:9002" "gateway:9000") do (
        for /f "tokens=1,2 delims=:" %%a in (%%e) do (
            set "svc_name=%%a"
            set "svc_port=%%b"
            set "svc_status=未运行"
            if exist "%PID_DIR%\%%a.pid" (
                set /p svc_pid=<"%PID_DIR%\%%a.pid"
                tasklist /FI "PID eq !svc_pid!" 2>nul | findstr "!svc_pid!" >nul 2>&1
                if not errorlevel 1 (
                    curl -s "http://localhost:%%b" >nul 2>&1
                    if not errorlevel 1 (
                        set "svc_status=运行中 (PID !svc_pid!, 端口 %%b)"
                    ) else (
                        set "svc_status=启动中 (PID !svc_pid!)"
                    )
                )
            )
            echo   %%a: !svc_status!
        )
    )
    echo.
    exit /b 0
