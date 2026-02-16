#!/bin/bash
#
# 一键启动脚本 — 全 Docker Compose 方案
#
# 用法:
#   ./run.sh              启动全部（构建 + 启动）
#   ./run.sh start        同上
#   ./run.sh stop         停止所有容器
#   ./run.sh restart      重启所有容器
#   ./run.sh status       查看容器状态
#   ./run.sh logs [svc]   查看日志（可指定服务名）
#   ./run.sh seed         导入种子数据
#   ./run.sh rebuild      重新编译 Java 服务并重启
#   ./run.sh down         停止并删除容器
#   ./run.sh clean        停止并删除容器 + 数据卷
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

COMPOSE_FILE="docker-compose.prod.yml"
PROJECT_NAME="live-commerce"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info()  { echo -e "${GREEN}[INFO]${NC} $*"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC} $*"; }
log_error() { echo -e "${RED}[ERROR]${NC} $*"; }

# ========== Pre-checks ==========

check_docker() {
    if ! command -v docker &>/dev/null; then
        log_error "未安装 Docker，请先安装: https://docs.docker.com/get-docker/"
        exit 1
    fi
    if ! docker info &>/dev/null; then
        log_error "Docker 未启动，请先启动 Docker"
        exit 1
    fi
    # Check docker compose (v2)
    if ! docker compose version &>/dev/null; then
        log_error "需要 Docker Compose V2，请升级 Docker"
        exit 1
    fi
}

# ========== Commands ==========

do_start() {
    check_docker
    log_info "构建并启动所有服务..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d --build

    echo ""
    log_info "所有服务已启动！等待健康检查完成..."
    echo ""

    # Wait for services to become healthy
    wait_healthy

    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}  服务启动完成！${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo "  前端页面:      http://localhost:${FRONTEND_PORT:-80}"
    echo "  Gateway:       http://localhost:${GATEWAY_PORT:-9000}"
    echo "  base-service:  http://localhost:9001"
    echo "  mall-service:  http://localhost:9002"
    echo "  SRS RTMP:      rtmp://localhost:${SRS_RTMP_PORT:-1935}/live/"
    echo "  SRS HTTP-FLV:  http://localhost:${SRS_HTTP_PORT:-8080}"
    echo ""
    echo "  提示: 运行 ./run.sh seed 导入示例数据"
    echo ""
}

do_stop() {
    check_docker
    log_info "停止所有容器..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" stop
    log_info "所有容器已停止"
}

do_restart() {
    check_docker
    log_info "重启所有容器..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" restart
    wait_healthy
    log_info "所有容器已重启"
}

do_status() {
    check_docker
    echo ""
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" ps
    echo ""
}

do_logs() {
    check_docker
    local service="$1"
    if [ -n "$service" ]; then
        docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs -f "$service"
    else
        docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" logs -f
    fi
}

do_seed() {
    check_docker
    log_info "导入种子数据..."

    # Check MySQL is running
    if ! docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" ps mysql | grep -q "running"; then
        log_error "MySQL 未运行，请先启动: ./run.sh start"
        exit 1
    fi

    # Wait a moment for services to create tables via JPA
    log_info "等待 JPA 自动建表..."
    sleep 5

    # Import seed data
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" exec -T mysql \
        mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-root123}" < sql/init.sql

    log_info "种子数据导入完成！"
    echo ""
    echo "  测试账号:"
    echo "    anchor1 / 123456 (美妆主播小美)"
    echo "    anchor2 / 123456 (数码达人老王)"
    echo ""
}

do_rebuild() {
    check_docker
    log_info "重新编译 Java 服务..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d --build \
        base-service mall-service gateway
    wait_healthy
    log_info "Java 服务重新编译并启动完成"
}

do_down() {
    check_docker
    log_info "停止并删除容器..."
    docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down
    log_info "容器已删除"
}

do_clean() {
    check_docker
    log_warn "将删除所有容器和数据卷（MySQL/Redis 数据将丢失）！"
    read -p "确认继续？(y/N) " confirm
    if [ "$confirm" = "y" ] || [ "$confirm" = "Y" ]; then
        docker compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down -v
        log_info "容器和数据卷已删除"
    else
        log_info "已取消"
    fi
}

wait_healthy() {
    local services=("lc-mysql" "lc-redis" "lc-base-service" "lc-mall-service" "lc-gateway")
    local max_wait=300
    local elapsed=0

    for svc in "${services[@]}"; do
        while [ $elapsed -lt $max_wait ]; do
            local status
            status=$(docker inspect --format='{{.State.Health.Status}}' "$svc" 2>/dev/null || echo "not_found")

            case "$status" in
                healthy)
                    log_info "$svc: 就绪"
                    break
                    ;;
                unhealthy)
                    log_warn "$svc: 不健康，查看日志: ./run.sh logs"
                    break
                    ;;
                not_found)
                    # Container might not exist yet or no healthcheck
                    break
                    ;;
                *)
                    sleep 5
                    elapsed=$((elapsed + 5))
                    ;;
            esac
        done
    done

    if [ $elapsed -ge $max_wait ]; then
        log_warn "部分服务启动超时，使用 ./run.sh status 查看状态"
    fi
}

# ========== Main ==========

case "${1:-start}" in
    start)    do_start ;;
    stop)     do_stop ;;
    restart)  do_restart ;;
    status)   do_status ;;
    logs)     do_logs "$2" ;;
    seed)     do_seed ;;
    rebuild)  do_rebuild ;;
    down)     do_down ;;
    clean)    do_clean ;;
    *)
        echo "用法: $0 {start|stop|restart|status|logs|seed|rebuild|down|clean}"
        echo ""
        echo "命令说明:"
        echo "  start     构建并启动全部服务"
        echo "  stop      停止所有容器"
        echo "  restart   重启所有容器"
        echo "  status    查看容器状态"
        echo "  logs      查看日志 (可加服务名: ./run.sh logs base-service)"
        echo "  seed      导入种子数据"
        echo "  rebuild   重新编译 Java 服务并重启"
        echo "  down      停止并删除容器"
        echo "  clean     删除容器 + 数据卷"
        exit 1
        ;;
esac
