# GitHub Secrets 配置指南

## 1. Repository Secrets

在 GitHub 仓库 `Settings → Secrets and variables → Actions → Repository secrets` 中添加:

| Secret 名称 | 值 | 说明 |
|------------|-----|------|
| `CI_DB_PASSWORD` | `RcTXVuulAtUsBmWFbSl3` | CI 测试用的临时数据库密码 |
| `CI_JWT_SECRET` | `MAqAYscEm3PnWo/54rLACl5xliyyqbAH0j6qjVHo18s=` | CI 测试用的 JWT 密钥 |

## 2. Environment Secrets

首先创建 Environment `dev`:
1. 进入 `Settings → Environments`
2. 点击 `New environment`
3. 输入名称 `dev`
4. 点击 `Configure environment`

然后在 Environment `dev` 中添加以下 secrets:

| Secret 名称 | 值 | 说明 |
|------------|-----|------|
| `DEPLOY_HOST` | `<服务器IP地址>` | 服务器 IP 地址 |
| `DEPLOY_PORT` | `22` | SSH 端口 |
| `DEPLOY_USER` | `<SSH用户名>` | SSH 登录用户 (如 root) |
| `DEPLOY_SSH_PRIVATE_KEY` | `<SSH私钥内容>` | 见下方生成方法 |
| `DEPLOY_KNOWN_HOSTS` | `<known_hosts内容>` | 见下方生成方法 |
| `DEPLOY_HEALTHCHECK_URL` | `http://<服务器IP>/healthz` | 健康检查 URL |

## 3. 生成 SSH 密钥对

在本地终端执行:

```bash
# 生成 SSH 密钥对
ssh-keygen -t ed25519 -C "github-actions-deploy" -f github-actions-deploy -N ""

# 查看私钥 (添加到 DEPLOY_SSH_PRIVATE_KEY)
cat github-actions-deploy

# 查看公钥 (添加到服务器的 ~/.ssh/authorized_keys)
cat github-actions-deploy.pub

# 获取 known_hosts (添加到 DEPLOY_KNOWN_HOSTS)
ssh-keyscan -p 22 <服务器IP>
```

## 4. 配置服务器 SSH

将公钥添加到服务器的 `~/.ssh/authorized_keys`:

```bash
# SSH 登录服务器
ssh <用户名>@<服务器IP>

# 添加公钥 (替换 <公钥内容>)
echo "<公钥内容>" >> ~/.ssh/authorized_keys

# 设置权限
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

## 5. 验证 SSH 连接

在本地测试 SSH 连接:

```bash
ssh -i github-actions-deploy <用户名>@<服务器IP> "echo 连接成功"
```

## 安全提示

- **不要提交此文件到 Git** - 此文件包含示例密钥,仅用于演示
- **定期轮换密钥** - 建议每 3-6 个月更换一次密钥
- **使用最小权限** - 为部署用户分配最小必要权限
