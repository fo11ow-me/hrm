# 部署检查清单

## 一、GitHub 配置 (在 GitHub 网页操作)

### Repository Secrets
- [ ] `CI_DB_PASSWORD` - CI 测试数据库密码
- [ ] `CI_JWT_SECRET` - CI 测试 JWT 密钥

### Environment `dev` Secrets
- [ ] `DEPLOY_HOST` - 服务器地址
- [ ] `DEPLOY_PORT` - SSH 端口 (22)
- [ ] `DEPLOY_USER` - SSH 用户名
- [ ] `DEPLOY_SSH_PRIVATE_KEY` - SSH 私钥
- [ ] `DEPLOY_KNOWN_HOSTS` - SSH known_hosts
- [ ] `DEPLOY_HEALTHCHECK_URL` - 健康检查 URL

---

## 二、服务器准备 (SSH 登录服务器操作)

### 2.1 检查现有环境
```bash
# 检查 Docker
docker --version
docker compose version

# 检查 MySQL/Redis 容器
docker ps | grep -E "mysql|redis"

# 检查网络
docker network ls
```

### 2.2 创建网络和目录
```bash
# 创建应用网络
docker network create hrm-network

# 将 MySQL/Redis 连接到网络
docker network connect hrm-network mysql8 2>/dev/null || true
docker network connect hrm-network redis5 2>/dev/null || true

# 创建目录
mkdir -p /opt/hrm /srv/hrm/files
```

### 2.3 准备数据库
```bash
# 创建数据库
docker exec mysql8 mysql -uroot -p<密码> -e "
CREATE DATABASE IF NOT EXISTS hrm CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS hrm_activiti CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
"
```

### 2.4 创建 .env 文件
```bash
cat > /opt/hrm/.env << 'EOF'
BACKEND_BIND_PORT=18888
FRONTEND_BIND_PORT=18080
EXTERNAL_DOCKER_NETWORK=hrm-network
FILE_STORAGE_HOST_PATH=/srv/hrm/files
PUBLIC_HEALTHCHECK_URL=https://<域名>/healthz

DB_MASTER_URL=jdbc:mysql://mysql8:3306/hrm?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2b8
DB_ACTIVITI_URL=jdbc:mysql://mysql8:3306/hrm_activiti?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2b8
DB_USERNAME=root
DB_PASSWORD=<MySQL密码>

REDIS_HOST=redis5
REDIS_PORT=6379
REDIS_PASSWORD=<Redis密码>
REDIS_DATABASE=0

JWT_SECRET=<Base64编码的JWT密钥>
ACTIVITI_SCHEMA_UPDATE=true
MYBATIS_LOG_IMPL=org.apache.ibatis.logging.nologging.NoLoggingImpl
ASSISTANT_ENABLED=false
EOF

chmod 600 /opt/hrm/.env
```

### 2.5 Docker 登录 GHCR
```bash
echo "<GitHub_Token>" | docker login ghcr.io -u <GitHub用户名> --password-stdin
```

---

## 三、Nginx 配置

### 3.1 创建配置
```bash
cat > /etc/nginx/sites-available/hrm << 'EOF'
server {
    listen 80;
    server_name <域名>;
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl http2;
    server_name <域名>;

    access_log /var/log/nginx/hrm-access.log;
    error_log /var/log/nginx/hrm-error.log warn;

    client_max_body_size 35m;

    location = /healthz {
        proxy_pass http://127.0.0.1:18888/actuator/health;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
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
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:18888/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location / {
        proxy_pass http://127.0.0.1:18080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
EOF
```

### 3.2 启用配置
```bash
ln -sf /etc/nginx/sites-available/hrm /etc/nginx/sites-enabled/
nginx -t && nginx -s reload
```

### 3.3 获取 SSL 证书 (可选)
```bash
certbot --nginx -d <域名>
```

---

## 四、推送代码触发部署

```bash
# 本地执行
git checkout dev
git add .
git commit -m "feat: prepare for deployment"
git push origin dev
```

---

## 五、部署验证

### 5.1 检查容器
```bash
docker ps | grep hrm
```

### 5.2 检查健康端点
```bash
curl http://127.0.0.1:18888/actuator/health
curl http://127.0.0.1:18080/healthz
```

### 5.3 检查日志
```bash
docker logs hrm-server-1 -f
```

### 5.4 浏览器访问
- [ ] `https://<域名>` 显示登录页
- [ ] 登录功能正常
- [ ] API 功能正常
