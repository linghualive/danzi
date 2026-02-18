@echo off
chcp 65001 >nul 2>&1
setlocal EnableExtensions EnableDelayedExpansion

set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

set "COMPOSE_FILE=docker-compose.prod.yml"
set "PROJECT_NAME=live-commerce"
set "IMAGES=live-commerce/base-service:latest live-commerce/mall-service:latest live-commerce/gateway:latest"

set "CMD=%~1"
if "%CMD%"=="" set "CMD=start"

if /I "%CMD%"=="start"   goto :do_start
if /I "%CMD%"=="stop"    goto :do_stop
if /I "%CMD%"=="restart" goto :do_restart
if /I "%CMD%"=="status"  goto :do_status
if /I "%CMD%"=="logs"    goto :do_logs
if /I "%CMD%"=="seed"    goto :do_seed
if /I "%CMD%"=="rebuild" goto :do_rebuild
if /I "%CMD%"=="down"    goto :do_down
if /I "%CMD%"=="clean"   goto :do_clean
if /I "%CMD%"=="export"  goto :do_export
goto :usage

:check_docker
where docker >nul 2>&1
if errorlevel 1 (
  echo [ERROR] 未安装 Docker，请先安装 Docker Desktop。
  exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
  echo [ERROR] Docker 未启动，请先启动 Docker。
  exit /b 1
)

docker compose version >nul 2>&1
if errorlevel 1 (
  echo [ERROR] 需要 Docker Compose V2，请升级 Docker。
  exit /b 1
)
exit /b 0

:do_start
call :check_docker || exit /b 1

if exist "images.tar" (
  echo [INFO] 检测到 images.tar，加载预构建镜像...
  docker load ^< images.tar
  if errorlevel 1 (
    echo [ERROR] 镜像加载失败。
    exit /b 1
  )
  del /f /q images.tar >nul 2>&1
  echo [INFO] 镜像加载完成。
)

if exist "Dockerfile" (
  echo [INFO] 构建并启动所有服务...
  docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" up -d --build
) else (
  echo [INFO] 启动所有服务（使用预构建镜像）...
  docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" up -d
)
if errorlevel 1 (
  echo [ERROR] docker compose up 执行失败。
  exit /b 1
)

echo.
echo [INFO] 所有服务已启动，等待健康检查完成...
echo.
call :wait_healthy
call :print_endpoints
echo [INFO] 提示: 运行 run.bat seed 导入示例数据
echo.
exit /b 0

:do_stop
call :check_docker || exit /b 1
echo [INFO] 停止所有容器...
docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" stop
if errorlevel 1 (
  echo [ERROR] docker compose stop 执行失败。
  exit /b 1
)
echo [INFO] 所有容器已停止。
exit /b 0

:do_restart
call :check_docker || exit /b 1
echo [INFO] 重启所有容器...
docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" restart
if errorlevel 1 (
  echo [ERROR] docker compose restart 执行失败。
  exit /b 1
)
call :wait_healthy
echo [INFO] 所有容器已重启。
exit /b 0

:do_status
call :check_docker || exit /b 1
echo.
docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" ps
echo.
exit /b 0

:do_logs
call :check_docker || exit /b 1
if "%~2"=="" (
  docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" logs -f
) else (
  docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" logs -f "%~2"
)
exit /b %errorlevel%

:do_seed
call :check_docker || exit /b 1
echo [INFO] 导入种子数据...

set "MYSQL_RUNNING="
for /f "delims=" %%s in ('docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" ps mysql 2^>nul ^| findstr /R /I "running Up"') do set "MYSQL_RUNNING=1"
if not defined MYSQL_RUNNING (
  echo [ERROR] MySQL 未运行，请先启动: run.bat start
  exit /b 1
)

echo [INFO] 等待 JPA 自动建表...
timeout /t 5 /nobreak >nul

set "MYSQL_ROOT_PW=%MYSQL_ROOT_PASSWORD%"
if not defined MYSQL_ROOT_PW set "MYSQL_ROOT_PW=root123"

echo [INFO] 清空旧数据...
docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" exec -T mysql mysql -uroot -p"%MYSQL_ROOT_PW%" ^< sql\clear_all_db.sql
if errorlevel 1 (
  echo [ERROR] 清空旧数据失败。
  exit /b 1
)

echo [INFO] 导入种子数据...
docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" exec -T mysql mysql -uroot -p"%MYSQL_ROOT_PW%" ^< sql\seed_test_data.sql
if errorlevel 1 (
  echo [ERROR] 导入种子数据失败。
  exit /b 1
)

echo [INFO] 种子数据导入完成！
echo.
echo   测试账号（密码均为 password123）:
echo     anchor1  (Anchor Amy      - 主播, 已有开播资格)
echo     anchor2  (Anchor Bob      - 主播, 已有开播资格)
echo     admin    (Platform Admin  - 管理员)
echo     buyer1   (Buyer Alice     - 普通用户)
echo     buyer2   (Buyer David     - 普通用户)
echo.
exit /b 0

:do_rebuild
call :check_docker || exit /b 1
echo [INFO] 重新编译 Java 服务...
docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" up -d --build base-service mall-service gateway
if errorlevel 1 (
  echo [ERROR] Java 服务重新编译失败。
  exit /b 1
)
call :wait_healthy
echo [INFO] Java 服务重新编译并启动完成。
exit /b 0

:do_down
call :check_docker || exit /b 1
echo [INFO] 停止并删除容器...
docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" down
if errorlevel 1 (
  echo [ERROR] docker compose down 执行失败。
  exit /b 1
)
echo [INFO] 容器已删除。
exit /b 0

:do_clean
call :check_docker || exit /b 1
echo [WARN] 将删除所有容器和数据卷（MySQL/Redis 数据将丢失）！
set /p CONFIRM=确认继续？(y/N) 
if /I not "%CONFIRM%"=="y" if /I not "%CONFIRM%"=="yes" (
  echo [INFO] 已取消。
  exit /b 0
)

docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" down -v
if errorlevel 1 (
  echo [ERROR] 清理失败。
  exit /b 1
)
echo [INFO] 容器和数据卷已删除。
exit /b 0

:do_export
call :check_docker || exit /b 1
echo [INFO] 开始构建并打包部署包...

echo [INFO] [1/4] 构建 Docker 镜像...
docker compose -f "%COMPOSE_FILE%" -p "%PROJECT_NAME%" build
if errorlevel 1 (
  echo [ERROR] 镜像构建失败。
  exit /b 1
)

echo [INFO] [2/4] 准备部署文件...
set "DIST_DIR=dist\live-commerce-docker"
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
mkdir "%DIST_DIR%" >nul 2>&1 || (
  echo [ERROR] 无法创建目录 %DIST_DIR%
  exit /b 1
)

xcopy /e /i /y frontend "%DIST_DIR%\frontend\" >nul
xcopy /e /i /y nginx "%DIST_DIR%\nginx\" >nul
xcopy /e /i /y sql "%DIST_DIR%\sql\" >nul
mkdir "%DIST_DIR%\srs" >nul 2>&1
copy /y srs\srs-docker.conf "%DIST_DIR%\srs\" >nul
copy /y run.sh "%DIST_DIR%\" >nul
copy /y run.bat "%DIST_DIR%\" >nul

powershell -NoProfile -Command "$in='docker-compose.prod.yml';$out='dist/live-commerce-docker/docker-compose.prod.yml';$skip=$false;Get-Content $in ^| ForEach-Object { if($_ -match '^\s{4}build:\s*$'){ $skip=$true; return }; if($skip -and $_ -match '^\s{6}target:\s*'){ $skip=$false; return }; if(-not $skip){ $_ } } ^| Set-Content -Encoding UTF8 $out"
if errorlevel 1 (
  echo [ERROR] 生成部署 compose 文件失败。
  exit /b 1
)

echo [INFO] [3/4] 导出 Docker 镜像（可能需要几分钟）...
docker save %IMAGES% > "%DIST_DIR%\images.tar"
if errorlevel 1 (
  echo [ERROR] 导出镜像失败。
  exit /b 1
)

echo [INFO] [4/4] 打包压缩...
if not exist "dist" mkdir dist >nul 2>&1
powershell -NoProfile -Command "Compress-Archive -Path 'dist/live-commerce-docker' -DestinationPath 'dist/live-commerce-docker.zip' -Force"
if errorlevel 1 (
  echo [ERROR] 压缩失败。
  exit /b 1
)

for %%I in ("dist\live-commerce-docker.zip") do set "ZIP_SIZE=%%~zI"
echo.
echo ========================================
echo   打包完成！
echo ========================================
echo.
echo   文件: dist\live-commerce-docker.zip (!ZIP_SIZE! bytes)
echo.
echo   部署到服务器:
echo     1. 上传 dist\live-commerce-docker.zip
echo     2. 解压后进入 live-commerce-docker 目录
echo     3. 运行 run.bat start
echo     4. 运行 run.bat seed 导入示例数据
echo.
exit /b 0

:wait_healthy
for %%s in (lc-mysql lc-redis lc-base-service lc-mall-service lc-gateway) do (
  call :wait_one %%s 300
)
exit /b 0

:wait_one
set "SERVICE=%~1"
set /a MAX_WAIT=%~2
set /a ELAPSED=0

:wait_one_loop
set "STATUS="
for /f "delims=" %%h in ('docker inspect --format "{{if .State.Health}}{{.State.Health.Status}}{{else}}no_healthcheck{{end}}" "!SERVICE!" 2^>nul') do set "STATUS=%%h"

if /I "!STATUS!"=="healthy" (
  echo [INFO] !SERVICE!: 就绪
  exit /b 0
)
if /I "!STATUS!"=="unhealthy" (
  echo [WARN] !SERVICE!: 不健康，查看日志: run.bat logs
  exit /b 0
)
if /I "!STATUS!"=="no_healthcheck" (
  echo [INFO] !SERVICE!: 未配置健康检查，跳过
  exit /b 0
)
if not defined STATUS (
  echo [WARN] !SERVICE!: 容器不存在，跳过
  exit /b 0
)

if !ELAPSED! geq !MAX_WAIT! (
  echo [WARN] !SERVICE!: 启动等待超时
  exit /b 0
)

timeout /t 5 /nobreak >nul
set /a ELAPSED+=5
goto :wait_one_loop

:print_endpoints
set "FRONTEND_PORT_SHOW=80"
if defined FRONTEND_PORT set "FRONTEND_PORT_SHOW=%FRONTEND_PORT%"
set "GATEWAY_PORT_SHOW=9000"
if defined GATEWAY_PORT set "GATEWAY_PORT_SHOW=%GATEWAY_PORT%"
set "SRS_RTMP_PORT_SHOW=1935"
if defined SRS_RTMP_PORT set "SRS_RTMP_PORT_SHOW=%SRS_RTMP_PORT%"
set "SRS_HTTP_PORT_SHOW=8080"
if defined SRS_HTTP_PORT set "SRS_HTTP_PORT_SHOW=%SRS_HTTP_PORT%"

echo.
echo ========================================
echo   服务启动完成！
echo ========================================
echo.
echo   前端页面:      http://localhost:!FRONTEND_PORT_SHOW!
echo   Gateway:       http://localhost:!GATEWAY_PORT_SHOW!
echo   base-service:  http://localhost:9001
echo   mall-service:  http://localhost:9002
echo   SRS RTMP:      rtmp://localhost:!SRS_RTMP_PORT_SHOW!/live/
echo   SRS HTTP-FLV:  http://localhost:!SRS_HTTP_PORT_SHOW!
echo.
exit /b 0

:usage
echo 用法: %~nx0 {start^|stop^|restart^|status^|logs^|seed^|rebuild^|down^|clean^|export}
echo.
echo 命令说明:
echo   start     构建并启动全部服务（服务器上自动加载预构建镜像）
echo   stop      停止所有容器
echo   restart   重启所有容器
echo   status    查看容器状态
echo   logs      查看日志 (可加服务名: run.bat logs gateway)
echo   seed      导入种子数据
echo   rebuild   重新编译 Java 服务并重启
echo   down      停止并删除容器
echo   clean     删除容器 + 数据卷
echo   export    本地构建并打包部署包
exit /b 1
