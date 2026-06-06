#!/bin/bash
# 在服务器上运行此脚本,收集现有 MySQL/Redis 配置信息
# 运行方式: bash server-info.sh

echo "=== Docker 容器信息 ==="
docker ps --format "table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Networks}}" | grep -E "mysql|redis|NAME"

echo ""
echo "=== Docker 网络信息 ==="
docker network ls

echo ""
echo "=== 检查常见网络中的容器 ==="
for net in my_network bridge host; do
  if docker network inspect $net >/dev/null 2>&1; then
    echo "网络 [$net] 中的容器:"
    docker network inspect $net --format '{{range .Containers}}{{.Name}} {{end}}' 2>/dev/null || echo "无容器"
  fi
done

echo ""
echo "=== MySQL 连接检查 ==="
echo "请手动确认 MySQL 用户名和密码"
echo "常见 MySQL 容器名: mysql, mysql8, mysql5"
echo ""
echo "尝试查看 MySQL 容器环境变量:"
docker inspect mysql8 2>/dev/null | grep -A5 "Env" | grep -E "MYSQL_ROOT_PASSWORD|MYSQL_USER|MYSQL_PASSWORD" || echo "未找到 mysql8 容器"

echo ""
echo "=== Redis 连接检查 ==="
echo "常见 Redis 容器名: redis, redis5"
echo ""
echo "尝试查看 Redis 容器配置:"
docker inspect redis5 2>/dev/null | grep -A10 "Cmd" | grep -E "redis-server|requirepass" || echo "未找到 redis5 容器"

echo ""
echo "=== 目录检查 ==="
ls -la /opt/hrm 2>/dev/null || echo "/opt/hrm 目录不存在"
ls -la /srv/hrm 2>/dev/null || echo "/srv/hrm 目录不存在"

echo ""
echo "=== Nginx 检查 ==="
ls -la /etc/nginx/sites-enabled/ 2>/dev/null || ls -la /etc/nginx/conf.d/ 2>/dev/null || echo "Nginx 配置目录未找到"