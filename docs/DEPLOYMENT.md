# CI/CD 部署指南

本文档描述如何将 HRM 项目部署到生产服务器。

## 概述

- **触发条件**: Push 到 `dev` 分支
- **镜像仓库**: GitHub Container Registry (ghcr.io)
- **部署方式**: SSH 远程执行 Docker Compose
- **回滚机制**: 自动回滚 + 手动回滚 workflow

## 流水线阶段

```
┌─────────────┐
│  Security   │ ─→ 敏感文件扫描、密钥检测
└─────────────┘
       │
       ├────────────────────────────────────┐
       ↓                                    ↓
┌─────────────┐                      ┌─────────────┐
│ Backend     │                      │ Frontend    │
│ Unit Tests  │                      │ Build       │
└─────────────┘                      └─────────────┘
       │
       ↓
┌─────────────┐
│ Backend     │ ─→ 需要 MySQL + Redis
│ Integration │
│ Tests       │
└─────────────┘
       │
       ↓
┌─────────────┐
│ Validate    │
│ Compose     │
└─────────────┘
       │
       ↓ (仅 push 事件)
┌─────────────┐
│ Publish     │ ─→ 推送到 ghcr.io
│ Images      │
└─────────────┘
       │
       ↓
┌─────────────┐
│ Deploy      │ ─→ SSH 部署
└─────────────┘
```

---

## 第一步: GitHub 配置

### 1.1 创建 Environment

在 GitHub 仓库中创建 Environment `dev`:

1. 进入仓库 Settings → Environments
2. 点击 "New environment"
3. 名称输入 `dev`
4. 可选: 配置部署审批规则

### 1.2 配置 Repository Secrets

进入 Settings → Secrets and variables → Actions → Repository secrets

| Secret 名称 | 说明 | 示例 |
|------------|------|------|
| `CI_DB_PASSWORD` | CI 测试用的 MySQL root 密码 | 随机生成的强密码 |
| `CI_JWT_SECRET` | CI 测试用的 JWT 密钥 | Base64 编码的 256 位密钥 |

### 1.3 配置 Environment Secrets

在 Environment `dev` 中添加以下 secrets:

| Secret 名称 | 说明 |
|------------|------|
| `DEPLOY_HOST` | 服务器 IP 地址 |
| `DEPLOY_PORT` | SSH 端口 (通常 22) |
| `DEPLOY_USER` | SSH 登录用户名 |
| `DEPLOY_SSH_PRIVATE_KEY` | SSH 私钥 (PEM 格式) |
| `DEPLOY_KNOWN_HOSTS` | SSH known_hosts 内容 |
| `DEPLOY_HEALTHCHECK_URL` | 公共健康检查 URL (如 `https://your-domain.com/healthz`) |

### 1.4 可选: Repository Variables

| Variable 名称 | 说明 | 默认值 |
|--------------|------|--------|
| `DEPLOY_PATH` | 服务器部署目录 | `/opt/hrm` |

### 1.5 生成 SSH 密钥对

```bash
# 在本地生成专用于部署的 SSH 密钥对
ssh-keygen -t ed25519 -C "github-actions-deploy" -f github-actions-deploy -N ""

# 输出私钥 (添加到 DEPLOY_SSH_PRIVATE_KEY)
cat github-actions-deploy

# 输出公钥 (添加到服务器的 ~/.ssh/authorized_keys)
cat github-actions-deploy.pub

# 获取 known_hosts (添加到 DEPLOY_KNOWN_HOSTS)
ssh-keyscan -p 22 <服务器地址>
```

---

## 第二步: 服务器准备

### 2.1 安装依赖

```bash
# 安装 Docker
curl -fsSL https://get.docker.com | sh
systemctl enable docker
systemctl start docker

# 安装 Docker Compose
apt-get update
apt-get install -y docker-compose-plugin

# 安装 Nginx
apt-get install -y nginx

# 安装 Certbot (用于 HTTPS)
apt-get install -y certbot python3-certbot-nginx
```

### 2.2 创建部署用户

```bash
# 创建专用部署用户
useradd -m -s /bin/bash hrm-deploy

# 允许该用户使用 Docker
usermod -aG docker hrm-deploy

# 配置 SSH 公钥
mkdir -p /home/hrm-deploy/.ssh
echo "github-actions-deploy.pub 的内容" >> /home/hrm-deploy/.ssh/authorized_keys
chmod 700 /home/hrm-deploy/.ssh
chmod 600 /home/hrm-deploy/.ssh/authorized_keys
chown -R hrm-deploy:hrm-deploy /home/hrm-deploy/.ssh
```

### 2.3 创建 Docker 网络

```bash
# 创建应用网络
docker network create hrm-network

# 如果 MySQL/Redis 在单独的网络,确保网络互通
# 查看现有网络
docker network ls

# 将 MySQL/Redis 连接到 hrm-network (如果需要)
docker network connect hrm-network mysql8
docker network connect hrm-network redis5
```

### 2.4 准备数据库

```bash
# 登录 MySQL 创建数据库
docker exec -it mysql8 mysql -uroot -p

# 在 MySQL 中执行
CREATE DATABASE IF NOT EXISTS hrm CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS hrm_activiti CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

# 导入 schema (如果有)
# USE hrm;
# SOURCE /path/to/hrm.sql;
```

### 2.5 创建部署目录

```bash
# 创建部署目录
mkdir -p /opt/hrm
mkdir -p /srv/hrm/files

# 设置权限
chown -R hrm-deploy:hrm-deploy /opt/hrm
chown -R hrm-deploy:hrm-deploy /srv/hrm
```

### 2.6 配置 .env 文件

在服务器上创建 `/opt/hrm/.env`:

```bash
# 在服务器上执行
cat > /opt/hrm/.env << 'EOF'
# 端口配置
BACKEND_BIND_PORT=18888
FRONTEND_BIND_PORT=18080

# Docker 网络
EXTERNAL_DOCKER_NETWORK=hrm-network

# 文件存储
FILE_STORAGE_HOST_PATH=/srv/hrm/files

# 公共健康检查 URL
PUBLIC_HEALTHCHECK_URL=https://your-domain.com/healthz

# 数据库配置 (根据实际情况修改)
DB_MASTER_URL=jdbc:mysql://mysql8:3306/hrm?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2b8
DB_ACTIVITI_URL=jdbc:mysql://mysql8:3306/hrm_activiti?useUnicode=true&characterEncoding=utf8&serverTimezone=GMT%2b8
DB_USERNAME=root
DB_PASSWORD=<你的MySQL密码>

# Redis 配置 (根据实际情况修改)
REDIS_HOST=redis5
REDIS_PORT=6379
REDIS_PASSWORD=<你的Redis密码>
REDIS_DATABASE=0

# JWT 密钥 (Base64 编码, 32字节以上)
JWT_SECRET=<Base64编码的JWT密钥>

# Activiti
ACTIVITI_SCHEMA_UPDATE=true

# MyBatis 日志
MYBATIS_LOG_IMPL=org.apache.ibatis.logging.nologging.NoLoggingImpl

# Assistant (可选)
ASSISTANT_ENABLED=false
EOF

# 设置权限
chmod 600 /opt/hrm/.env
```

### 2.7 配置 Nginx

创建 Nginx 配置 `/etc/nginx/sites-available/hrm`:

```nginx
# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name your-domain.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

# HTTPS
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # 日志
    access_log /var/log/nginx/hrm-access.log;
    error_log /var/log/nginx/hrm-error.log warn;

    # SSL 证书 (首次部署时先注释掉,用 Certbot 获取证书后再启用)
    # ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    # ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # 上传大小限制
    client_max_body_size 35m;

    # 健康检查
    location = /healthz {
        proxy_pass http://127.0.0.1:18888/actuator/health;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # SSE 端点 (文件任务进度)
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

    # API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:18888/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 前端
    location / {
        proxy_pass http://127.0.0.1:18080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用配置:

```bash
# 创建软链接
ln -s /etc/nginx/sites-available/hrm /etc/nginx/sites-enabled/

# 测试配置
nginx -t

# 重载 Nginx
nginx -s reload
```

### 2.8 获取 SSL 证书 (如果有域名)

```bash
# 使用 Certbot 获取 Let's Encrypt 证书
certbot --nginx -d your-domain.com

# 自动续期
systemctl enable certbot.timer
```

### 2.9 Docker 登录 GHCR

```bash
# 在服务器上登录 GitHub Container Registry
# 需要先在 GitHub 创建一个 Personal Access Token (read:packages 权限)
echo "YOUR_GITHUB_TOKEN" | docker login ghcr.io -u YOUR_GITHUB_USERNAME --password-stdin
```

---

## 第三步: 推送代码触发部署

### 3.1 本地准备

```bash
# 确保在正确的分支
git checkout dev

# 添加所有更改
git add .

# 提交
git commit -m "feat: prepare for deployment"

# 推送到 GitHub
git push origin dev
```

### 3.2 查看 Actions 运行状态

1. 进入 GitHub 仓库 → Actions
2. 查看最新的 workflow 运行
3. 确认所有步骤通过

### 3.3 部署验证

部署完成后,验证以下内容:

```bash
# 在服务器上检查容器状态
docker compose -f /opt/hrm/docker-compose.prod.yml --env-file /opt/hrm/.env ps

# 检查健康端点
curl http://127.0.0.1:18888/actuator/health
curl http://127.0.0.1:18080/healthz

# 检查公共访问
curl https://your-domain.com/healthz
```

---

## 第四步: 验证部署

### 4.1 功能验证清单

- [ ] 访问 `https://your-domain.com` 能看到登录页面
- [ ] 使用测试账号登录成功
- [ ] 访问 `/swagger-ui.html` 能看到 API 文档
- [ ] 文件上传功能正常
- [ ] 数据导入/导出功能正常
- [ ] SSE 文件任务进度推送正常

### 4.2 日志查看

```bash
# 查看后端日志
docker logs hrm-server-1 -f

# 查看前端日志
docker logs hrm-admin-1 -f

# 查看 Nginx 日志
tail -f /var/log/nginx/hrm-access.log
tail -f /var/log/nginx/hrm-error.log
```

---

## 回滚

### 自动回滚

部署脚本在健康检查失败时会自动回滚到上一个版本。

### 手动回滚

1. 进入 GitHub 仓库 → Actions
2. 选择 "Rollback dev" workflow
3. 点击 "Run workflow"
4. 输入之前的镜像标签 (如 `sha-abc123...`)
5. 点击 "Run workflow"

---

## 常见问题

### Q: 部署失败,容器无法启动

检查服务器上的日志:
```bash
docker logs hrm-server-1
docker logs hrm-admin-1
```

常见原因:
- 数据库连接失败: 检查 `.env` 中的数据库配置和网络
- Redis 连接失败: 检查 Redis 配置和网络
- 端口冲突: 检查 18888 和 18080 端口是否被占用

### Q: 前端无法访问后端 API

检查 Nginx 配置和网络:
```bash
# 测试后端直接访问
curl http://127.0.0.1:18888/actuator/health

# 检查 Nginx 配置
nginx -t

# 查看 Nginx 错误日志
tail -f /var/log/nginx/hrm-error.log
```

### Q: 文件上传失败

检查文件存储目录权限:
```bash
ls -la /srv/hrm/files
# 确保容器有写入权限
```

---

## 安全注意事项

1. **永远不要提交敏感信息到 Git**
   - `.env` 文件已添加到 `.gitignore`
   - `application-prod.yml` 已添加到 `.gitignore`
   - 使用 GitHub Secrets 存储敏感配置

2. **定期轮换密钥**
   - JWT 密钥
   - 数据库密码
   - SSH 密钥

3. **限制服务器访问**
   - 配置防火墙规则
   - 禁用密码登录,仅允许 SSH 密钥
   - 定期更新系统和 Docker

4. **监控和日志**
   - 定期检查日志
   - 设置告警通知
