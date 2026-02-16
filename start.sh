#!/bin/bash
#
# 一键启动直播电商平台
# 用法: ./start.sh [命令]
#   ./start.sh          - 启动中间件 + 编译 + 启动所有服务
#   ./start.sh infra    - 只启动中间件 (MySQL/Redis/SRS)
#   ./start.sh build    - 只编译项目
#   ./start.sh services - 只启动 3 个微服务 (需中间件已运行)
#   ./start.sh stop     - 停止所有服务和中间件
#   ./start.sh status   - 查看运行状态
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# PID 文件目录
PID_DIR="$SCRIPT_DIR/.pids"
mkdir -p "$PID_DIR"

log_info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }
log_step()  { echo -e "${BLUE}[STEP]${NC} $1"; }

# ============================================================
# 检查依赖
# ============================================================
check_deps() {
    local missing=()
    command -v docker >/dev/null 2>&1 || missing+=("docker")
    command -v java >/dev/null 2>&1   || missing+=("java (JDK 17+)")

    if [ ${#missing[@]} -gt 0 ]; then
        log_error "缺少依赖: ${missing[*]}"
        exit 1
    fi

    # 检查 Docker 是否在运行
    if ! docker info >/dev/null 2>&1; then
        log_error "Docker 未运行，请先启动 Docker Desktop"
        exit 1
    fi

    # 检查 Java 版本
    java_version=$(java -version 2>&1 | head -1 | awk -F '"' '{print $2}' | cut -d. -f1)
    if [ "$java_version" -lt 17 ] 2>/dev/null; then
        log_error "需要 JDK 17+，当前: $(java -version 2>&1 | head -1)"
        exit 1
    fi
}

# ============================================================
# 启动中间件 (MySQL + Redis + SRS)
# ============================================================
start_infra() {
    log_step "启动中间件 (MySQL + Redis + SRS)..."
    docker compose up -d

    log_info "等待 MySQL 就绪..."
    local retries=0
    until docker exec live-commerce-mysql mysqladmin ping -h localhost -u root -proot123 --silent 2>/dev/null; do
        retries=$((retries + 1))
        if [ $retries -ge 30 ]; then
            log_error "MySQL 启动超时"
            exit 1
        fi
        sleep 2
    done
    log_info "MySQL 就绪"

    log_info "等待 Redis 就绪..."
    retries=0
    until docker exec live-commerce-redis redis-cli ping 2>/dev/null | grep -q PONG; do
        retries=$((retries + 1))
        if [ $retries -ge 15 ]; then
            log_error "Redis 启动超时"
            exit 1
        fi
        sleep 1
    done
    log_info "Redis 就绪"

    log_info "中间件全部启动完成"
    echo ""
    echo "  MySQL:  localhost:3306 (root/root123)"
    echo "  Redis:  localhost:6379"
    echo "  SRS:    RTMP=localhost:1935  HTTP-FLV=localhost:8080  API=localhost:1985"
    echo ""
}

# ============================================================
# 编译项目
# ============================================================
build_project() {
    log_step "编译项目..."
    ./mvnw clean package -DskipTests -q
    log_info "编译完成"
}

# ============================================================
# 启动微服务
# ============================================================
start_service() {
    local name=$1
    local module=$2
    local port=$3

    # 检查端口是否被占用
    if lsof -i :"$port" -sTCP:LISTEN >/dev/null 2>&1; then
        log_warn "$name (端口 $port) 已在运行，跳过"
        return
    fi

    log_info "启动 $name (端口 $port)..."
    nohup java -jar "$module/target/$module-1.0.0-SNAPSHOT.jar" \
        > "$PID_DIR/$name.log" 2>&1 &
    echo $! > "$PID_DIR/$name.pid"
}

wait_for_service() {
    local name=$1
    local port=$2
    local retries=0

    while ! curl -s "http://localhost:$port" >/dev/null 2>&1; do
        retries=$((retries + 1))
        if [ $retries -ge 60 ]; then
            log_error "$name 启动超时，查看日志: $PID_DIR/$name.log"
            return 1
        fi
        sleep 2
    done
    log_info "$name 启动成功"
}

start_services() {
    log_step "启动微服务..."

    start_service "base-service" "base-service" 9001
    start_service "mall-service" "mall-service" 9002

    # 等待后端服务就绪后再启动网关
    wait_for_service "base-service" 9001
    wait_for_service "mall-service" 9002

    start_service "gateway" "gateway" 9000
    wait_for_service "gateway" 9000

    echo ""
    log_info "所有服务启动完成!"
    echo ""
    echo "  =========================================="
    echo "  Gateway:       http://localhost:9000"
    echo "  base-service:  http://localhost:9001"
    echo "  mall-service:  http://localhost:9002"
    echo "  =========================================="
    echo "  Swagger UI:"
    echo "    base-service: http://localhost:9001/swagger-ui.html"
    echo "    mall-service: http://localhost:9002/swagger-ui.html"
    echo "  =========================================="
    echo "  推流地址: rtmp://localhost:1935/live/{streamKey}"
    echo "  拉流地址: http://localhost:8080/live/{streamKey}.flv"
    echo "  =========================================="
    echo ""
    echo "  停止服务: ./start.sh stop"
    echo "  查看状态: ./start.sh status"
    echo "  查看日志: tail -f .pids/base-service.log"
    echo ""
}

# ============================================================
# 停止
# ============================================================
stop_services() {
    log_step "停止微服务..."
    for name in gateway mall-service base-service; do
        local pid_file="$PID_DIR/$name.pid"
        if [ -f "$pid_file" ]; then
            local pid=$(cat "$pid_file")
            if kill -0 "$pid" 2>/dev/null; then
                kill "$pid"
                log_info "已停止 $name (PID $pid)"
            fi
            rm -f "$pid_file"
        fi
    done
}

stop_infra() {
    log_step "停止中间件..."
    docker compose down
    log_info "中间件已停止"
}

stop_all() {
    stop_services
    stop_infra
    log_info "全部已停止"
}

# ============================================================
# 状态
# ============================================================
show_status() {
    echo ""
    echo "=== 中间件状态 ==="
    docker compose ps 2>/dev/null || echo "  Docker Compose 未运行"
    echo ""
    echo "=== 微服务状态 ==="
    for entry in "base-service:9001" "mall-service:9002" "gateway:9000"; do
        local name="${entry%%:*}"
        local port="${entry##*:}"
        local pid_file="$PID_DIR/$name.pid"
        local status="${RED}未运行${NC}"

        if [ -f "$pid_file" ] && kill -0 "$(cat "$pid_file")" 2>/dev/null; then
            if curl -s "http://localhost:$port" >/dev/null 2>&1; then
                status="${GREEN}运行中${NC} (PID $(cat "$pid_file"), 端口 $port)"
            else
                status="${YELLOW}启动中${NC} (PID $(cat "$pid_file"))"
            fi
        fi
        echo -e "  $name: $status"
    done
    echo ""
}

# ============================================================
# 主入口
# ============================================================
case "${1:-all}" in
    infra)
        check_deps
        start_infra
        ;;
    build)
        build_project
        ;;
    services)
        start_services
        ;;
    stop)
        stop_all
        ;;
    status)
        show_status
        ;;
    all)
        check_deps
        start_infra
        build_project
        start_services
        ;;
    *)
        echo "用法: $0 {all|infra|build|services|stop|status}"
        exit 1
        ;;
esac
