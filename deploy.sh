#!/bin/bash
#
# 增强部署脚本 — JAR 部署方案
#
# 用法:
#   ./deploy.sh start      启动中间件 + 微服务（全自动）
#   ./deploy.sh stop       停止微服务
#   ./deploy.sh stop-all   停止微服务 + 中间件
#   ./deploy.sh restart    重启微服务
#   ./deploy.sh status     查看运行状态
#   ./deploy.sh logs       查看最近日志
#
# 前提: 服务器已安装 JDK 17+ 和 Docker
#

set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

PID_DIR="$SCRIPT_DIR/.pids"
LOG_DIR="$SCRIPT_DIR/logs"
mkdir -p "$PID_DIR" "$LOG_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ========== Pre-checks ==========

check_jars() {
    local missing=0
    for jar in base-service-1.0.0-SNAPSHOT.jar mall-service-1.0.0-SNAPSHOT.jar gateway-1.0.0-SNAPSHOT.jar; do
        if [ ! -f "lib/$jar" ]; then
            log_error "缺少 lib/$jar，请先打包: ./start.sh package"
            missing=1
        fi
    done
    if [ $missing -eq 1 ]; then
        exit 1
    fi
}

# ========== Infrastructure ==========

start_infra() {
    if ! command -v docker &>/dev/null; then
        log_error "未安装 Docker，中间件需要 Docker 运行"
        exit 1
    fi

    if ! docker info &>/dev/null; then
        log_error "Docker 未启动，请先启动 Docker"
        exit 1
    fi

    # Check if middleware containers are already running
    local mysql_running=false redis_running=false srs_running=false

    if docker ps --format '{{.Names}}' | grep -q 'live-commerce-mysql'; then
        mysql_running=true
    fi
    if docker ps --format '{{.Names}}' | grep -q 'live-commerce-redis'; then
        redis_running=true
    fi
    if docker ps --format '{{.Names}}' | grep -q 'live-commerce-srs'; then
        srs_running=true
    fi

    if $mysql_running && $redis_running && $srs_running; then
        log_info "中间件已在运行"
    else
        log_info "启动中间件 (MySQL, Redis, SRS)..."
        if [ -f "docker-compose.yml" ]; then
            docker compose up -d
        else
            log_error "找不到 docker-compose.yml"
            exit 1
        fi
    fi

    # Wait for MySQL
    log_info "等待 MySQL 就绪..."
    local retries=0
    while ! docker exec live-commerce-mysql mysqladmin ping -uroot -proot123 --silent 2>/dev/null; do
        retries=$((retries + 1))
        if [ $retries -ge 60 ]; then
            log_error "MySQL 启动超时"
            exit 1
        fi
        sleep 2
    done
    log_info "MySQL 就绪"

    # Wait for Redis
    log_info "等待 Redis 就绪..."
    retries=0
    while ! docker exec live-commerce-redis redis-cli ping 2>/dev/null | grep -q PONG; do
        retries=$((retries + 1))
        if [ $retries -ge 30 ]; then
            log_error "Redis 启动超时"
            exit 1
        fi
        sleep 2
    done
    log_info "Redis 就绪"
}

stop_infra() {
    if command -v docker &>/dev/null && docker info &>/dev/null; then
        if [ -f "docker-compose.yml" ]; then
            log_info "停止中间件..."
            docker compose stop
            log_info "中间件已停止"
        fi
    fi
}

# ========== Services ==========

start_svc() {
    local name=$1 jar=$2 port=$3

    if [ -f "$PID_DIR/$name.pid" ]; then
        local old_pid
        old_pid=$(cat "$PID_DIR/$name.pid")
        if kill -0 "$old_pid" 2>/dev/null; then
            log_warn "$name 已在运行 (PID $old_pid)，跳过"
            return
        fi
    fi

    local conf_arg=""
    if [ -f "conf/${name}.yml" ]; then
        conf_arg="--spring.config.location=file:conf/${name}.yml"
    fi

    log_info "启动 $name (端口 $port)..."
    nohup java -jar "lib/$jar" $conf_arg \
        > "$LOG_DIR/$name.log" 2>&1 &
    echo $! > "$PID_DIR/$name.pid"

    # Wait for service to be ready
    local retries=0
    while ! curl -s "http://localhost:$port" >/dev/null 2>&1; do
        retries=$((retries + 1))
        if [ $retries -ge 60 ]; then
            log_error "$name 启动超时，查看日志: $LOG_DIR/$name.log"
            return 1
        fi
        sleep 2
    done
    log_info "$name 启动成功 (PID $(cat "$PID_DIR/$name.pid"))"
}

stop_svc() {
    local name=$1
    if [ -f "$PID_DIR/$name.pid" ]; then
        local pid
        pid=$(cat "$PID_DIR/$name.pid")
        if kill -0 "$pid" 2>/dev/null; then
            log_info "停止 $name (PID $pid)..."
            kill "$pid"
            # Wait for graceful shutdown
            local retries=0
            while kill -0 "$pid" 2>/dev/null; do
                retries=$((retries + 1))
                if [ $retries -ge 15 ]; then
                    log_warn "$name 未能优雅停止，强制终止"
                    kill -9 "$pid" 2>/dev/null || true
                    break
                fi
                sleep 1
            done
            log_info "$name 已停止"
        fi
        rm -f "$PID_DIR/$name.pid"
    fi
}

do_start() {
    check_jars
    start_infra

    # Auto-import SQL if needed (first run)
    auto_seed

    start_svc "base-service" "base-service-1.0.0-SNAPSHOT.jar" 9001
    start_svc "mall-service" "mall-service-1.0.0-SNAPSHOT.jar" 9002
    start_svc "gateway" "gateway-1.0.0-SNAPSHOT.jar" 9000

    echo ""
    log_info "所有服务启动完成"
    echo "  Gateway:       http://localhost:9000"
    echo "  base-service:  http://localhost:9001"
    echo "  mall-service:  http://localhost:9002"
    echo ""
}

do_stop() {
    stop_svc "gateway"
    stop_svc "mall-service"
    stop_svc "base-service"
    log_info "所有微服务已停止"
}

do_stop_all() {
    do_stop
    stop_infra
    log_info "所有服务（含中间件）已停止"
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
    echo "=== 中间件状态 ==="
    if command -v docker &>/dev/null && docker info &>/dev/null 2>&1; then
        for c in "live-commerce-mysql:MySQL" "live-commerce-redis:Redis" "live-commerce-srs:SRS"; do
            local cname="${c%%:*}"
            local label="${c##*:}"
            if docker ps --format '{{.Names}}' | grep -q "$cname"; then
                echo "  $label: 运行中"
            else
                echo "  $label: 未运行"
            fi
        done
    else
        echo "  Docker 不可用"
    fi
    echo ""
}

do_logs() {
    echo ""
    for name in base-service mall-service gateway; do
        if [ -f "$LOG_DIR/$name.log" ]; then
            echo "=== $name (最后 20 行) ==="
            tail -20 "$LOG_DIR/$name.log"
            echo ""
        fi
    done
}

auto_seed() {
    # Check if base_db has tables (first run detection)
    local has_tables
    has_tables=$(docker exec live-commerce-mysql mysql -uroot -proot123 -N -e \
        "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='base_db'" 2>/dev/null || echo "0")

    if [ "$has_tables" = "0" ]; then
        log_info "首次启动，将在服务建表后自动导入种子数据"
        log_info "请在服务启动后运行: mysql -uroot -proot123 < sql/init.sql"
    fi
}

case "${1:-start}" in
    start)    do_start ;;
    stop)     do_stop ;;
    stop-all) do_stop_all ;;
    restart)  do_stop; sleep 2; do_start ;;
    status)   do_status ;;
    logs)     do_logs ;;
    *)
        echo "用法: $0 {start|stop|stop-all|restart|status|logs}"
        echo ""
        echo "命令说明:"
        echo "  start      启动中间件 + 微服务（全自动）"
        echo "  stop       停止微服务"
        echo "  stop-all   停止微服务 + 中间件"
        echo "  restart    重启微服务"
        echo "  status     查看运行状态"
        echo "  logs       查看最近日志"
        exit 1
        ;;
esac
