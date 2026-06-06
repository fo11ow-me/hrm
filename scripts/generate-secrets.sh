#!/bin/bash
# 生成部署所需的密钥和配置
# 运行方式: bash scripts/generate-secrets.sh

set -e

echo "=== HRM 部署密钥生成工具 ==="
echo ""

# 1. 生成 JWT 密钥 (Base64 编码的 32 字节随机数)
echo "1. JWT 密钥:"
JWT_SECRET=$(openssl rand -base64 32)
echo "JWT_SECRET=$JWT_SECRET"
echo ""

# 2. 生成 CI 测试用的数据库密码
echo "2. CI 数据库密码:"
CI_DB_PASSWORD=$(openssl rand -base64 16 | tr -d '/+=' | head -c 20)
echo "CI_DB_PASSWORD=$CI_DB_PASSWORD"
echo ""

# 3. 生成 CI 测试用的 JWT 密钥
echo "3. CI JWT 密钥:"
CI_JWT_SECRET=$(openssl rand -base64 32)
echo "CI_JWT_SECRET=$CI_JWT_SECRET"
echo ""

# 4. 生成 SSH 密钥对
echo "4. SSH 密钥对 (用于 GitHub Actions 部署):"
echo "执行以下命令生成 SSH 密钥对:"
echo ""
echo "  ssh-keygen -t ed25519 -C 'github-actions-deploy' -f github-actions-deploy -N ''"
echo ""
echo "然后将:"
echo "  - 私钥 (github-actions-deploy) 添加到 GitHub Environment Secret: DEPLOY_SSH_PRIVATE_KEY"
echo "  - 公钥 (github-actions-deploy.pub) 添加到服务器的 ~/.ssh/authorized_keys"
echo ""

# 5. 生成 known_hosts
echo "5. SSH known_hosts:"
echo "执行以下命令获取服务器 known_hosts:"
echo ""
echo "  ssh-keyscan -p 22 <服务器地址>"
echo ""
echo "将输出内容添加到 GitHub Environment Secret: DEPLOY_KNOWN_HOSTS"
echo ""

echo "=== 完成 ==="
echo ""
echo "将以上生成的值添加到 GitHub Secrets:"
echo "  Repository Secrets: CI_DB_PASSWORD, CI_JWT_SECRET"
echo "  Environment 'dev' Secrets: DEPLOY_HOST, DEPLOY_PORT, DEPLOY_USER, DEPLOY_SSH_PRIVATE_KEY, DEPLOY_KNOWN_HOSTS, DEPLOY_HEALTHCHECK_URL"
