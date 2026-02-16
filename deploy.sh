#!/bin/bash
#
# 部署启动脚本 — 服务器上使用
# 用法:
#   ./deploy.sh start    - 启动所有微服务
#   ./deploy.sh stop     - 停止所有微服务
#   ./deploy.sh restart  - 重启所有微服务
#   ./deploy.sh status   - 查看运行状态
#
# 前提: 服务器已安装 JDK 17+，MySQL/Redis/SRS 已启动
# 如需修改中间件地址，编辑 conf/ 下对应的 yml 文件
#

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

PID_DIR="$SCRIPT_DIR/.pids"
LOG_DIR="$SCRIPT_DIR/logs"
mkdir -p "$PID_DIR" "$LOG_DIR"

start_svc() {
    local name=$1 jar=$2 port=$3 conf=$4

    if [ -f "$PID_DIR/$name.pid" ]; then
        local old_pid=$(cat "$PID_DIR/$name.pid")
        if kill -0 "$old_pid" 2>/dev/null; then
            echo "[WARN] $name 已在运行 (PID $old_pid)，跳过"
            return
        fi
    fi

    echo "[INFO] 启动 $name (端口 $port)..."
    nohup java -jar "lib/$jar" --spring.config.location="file:conf/$conf" \
        > "$LOG_DIR/$name.log" 2>&1 &
    echo $! > "$PID_DIR/$name.pid"

    local retries=0
    while ! curl -s "http://localhost:$port" >/dev/null 2>&1; do
        retries=$((retries + 1))
        if [ $retries -ge 60 ]; then
            echo "[ERROR] $name 启动超时，查看日志: $LOG_DIR/$name.log"
            return 1
        fi
        sleep 2
    done
    echo "[INFO] $name 启动成功 (PID $(cat "$PID_DIR/$name.pid"))"
}

stop_svc() {
    local name=$1
    if [ -f "$PID_DIR/$name.pid" ]; then
        local pid=$(cat "$PID_DIR/$name.pid")
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid"
            echo "[INFO] 已停止 $name (PID $pid)"
        fi
        rm -f "$PID_DIR/$name.pid"
    fi
}

do_start() {
    start_svc "base-service" "base-service-1.0.0-SNAPSHOT.jar" 9001 "base-service.yml"
    start_svc "mall-service" "mall-service-1.0.0-SNAPSHOT.jar" 9002 "mall-service.yml"
    start_svc "gateway" "gateway-1.0.0-SNAPSHOT.jar" 9000 "gateway.yml"
    echo ""
    echo "[INFO] 所有服务启动完成"
    echo "  Gateway:       http://localhost:9000"
    echo "  base-service:  http://localhost:9001"
    echo "  mall-service:  http://localhost:9002"
    echo ""
}

do_stop() {
    stop_svc "gateway"
    stop_svc "mall-service"
    stop_svc "base-service"
    echo "[INFO] 所有服务已停止"
}

do_status() {
    echo ""
    echo "=== 微服务状态 ==="
    for entry in "base-service:9001" "mall-service:9002" "gateway:9000"; do
        local name="${entry%%:*}"
        local port="${entry##*:}"
        local status="未运行"
        if [ -f "$PID_DIR/$name.pid" ] && kill -0 "$(cat "$PID_DIR/$name.pid")" 2>/dev/null; then
            if curl -s "http://localhost:$port" >/dev/null 2>&1; then
                status="运行中 (PID $(cat "$PID_DIR/$name.pid"), 端口 $port)"
            else
                status="启动中 (PID $(cat "$PID_DIR/$name.pid"))"
            fi
        fi
        echo "  $name: $status"
    done
    echo ""
}

case "${1:-start}" in
    start)   do_start ;;
    stop)    do_stop ;;
    restart) do_stop; sleep 2; do_start ;;
    status)  do_status ;;
    *)       echo "用法: $0 {start|stop|restart|status}"; exit 1 ;;
esac
