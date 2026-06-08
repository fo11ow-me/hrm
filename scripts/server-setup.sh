# 服务器配置命令
# 请 SSH 登录服务器后按顺序执行

# ============================================================
# 1. 检查现有环境
# ============================================================

# 检查 Docker
docker --version
docker compose version

# 检查 MySQL/Redis 容器
docker ps | grep -E "mysql|redis"

# 检查网络
docker network ls

# ============================================================
# 2. 创建 Docker 网络
# ============================================================

# 创建应用网络
docker network create hrm-network

# 将 MySQL/Redis 连接到网络 (替换容器名如果不同)
docker network connect hrm-network mysql8 2>/dev/null || docker network connect hrm-network mysql
docker network connect hrm-network redis5 2>/dev/null || docker network connect hrm-network redis

# ============================================================
# 3. 准备数据库 (替换 <MySQL密码> 为实际密码)
# ============================================================

docker exec mysql8 mysql -uroot -p<MySQL密码> -e "
CREATE DATABASE IF NOT EXISTS hrm CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS hrm_activiti CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
" 2>/dev/null || docker exec mysql mysql -uroot -p<MySQL密码> -e "
CREATE DATABASE IF NOT EXISTS hrm CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS hrm_activiti CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
"

# ============================================================
# 4. 创建目录
# ============================================================

mkdir -p /opt/hrm /srv/hrm/files

# ============================================================
# 5. 创建 .env 文件 (替换所有 <...> 占位符)
# ============================================================

cat > /opt/hrm/.env << 'EOF'
BACKEND_BIND_PORT=18888
FRONTEND_BIND_PORT=18080
EXTERNAL_DOCKER_NETWORK=hrm-network
FILE_STORAGE_HOST_PATH=/srv/hrm/files
PUBLIC_HEALTHCHECK_URL=http://<服务器IP>/healthz

DB_MASTER_URL=jdbc:mysql://mysql8:3306/hrm?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2b8
DB_ACTIVITI_URL=jdbc:mysql://mysql8:3306/hrm_activiti?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2b8
DB_USERNAME=root
DB_PASSWORD=<MySQL密码>

REDIS_HOST=redis5
REDIS_PORT=6379
REDIS_PASSWORD=<Redis密码>
REDIS_DATABASE=0

JWT_SECRET=<JWT密钥>
ACTIVITI_SCHEMA_UPDATE=true
MYBATIS_LOG_IMPL=org.apache.ibatis.logging.nologging.NoLoggingImpl
ASSISTANT_ENABLED=false
EOF

chmod 600 /opt/hrm/.env

# ============================================================
# 6. Docker 登录 GHCR (替换 <GitHub_Token> 和 <用户名>)
# ============================================================

# 需先在 GitHub 创建 Personal Access Token (read:packages 权限)
echo "<GitHub_Token>" | docker login ghcr.io -u <GitHub用户名> --password-stdin

# ============================================================
# 7. Nginx 配置
# ============================================================

# 创建配置文件 (如果有域名,替换 <域名>)
cat > /etc/nginx/sites-available/hrm << 'EOFNGINX'
server {
    listen 80;
    server_name <服务器IP>;

    client_max_body_size 35m;

    location = /healthz {
        proxy_pass http://127.0.0.1:18888/actuator/health;
        proxy_set_header Host $host;
    }

    location = /api/file-task/subscribe {
        access_log off;
        proxy_buffering off;
        proxy_cache off;
        proxy_read_timeout 35m;
        proxy_pass http://127.0.0.1:18888/file-task/subscribe;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host $host;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:18888/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location / {
        proxy_pass http://127.0.0.1:18080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
    }
}
EOFNGINX

# 启用配置
ln -sf /etc/nginx/sites-available/hrm /etc/nginx/sites-enabled/

# 测试并重载
nginx -t && nginx -s reload