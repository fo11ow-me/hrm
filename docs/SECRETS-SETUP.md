# GitHub Secrets 配置清单

请按照以下步骤在 GitHub 仓库中配置 Secrets。

## 第一步: 创建 Environment

1. 打开 GitHub 仓库: https://github.com/fellow-me/hrm
2. 进入 `Settings` → `Environments`
3. 点击 `New environment`
4. 输入名称: `dev`
5. 点击 `Configure environment`

## 第二步: 配置 Repository Secrets

进入 `Settings` → `Secrets and variables` → `Actions` → `Repository secrets`

点击 `New repository secret` 添加以下内容:

### CI_DB_PASSWORD
```
RcTXVuulAtUsBmWFbSl3
```

### CI_JWT_SECRET
```
MAqAYscEm3PnWo/54rLACl5xliyyqbAH0j6qjVHo18s=
```

## 第三步: 配置 Environment Secrets

进入 `Settings` → `Environments` → `dev` → `Environment secrets`

点击 `Add secret` 添加以下内容:

### DEPLOY_HOST
```
47.106.93.24
```

### DEPLOY_PORT
```
22
```

### DEPLOY_USER
```
root
```
(如果你的 SSH 用户名不是 root,请修改为实际用户名)

### DEPLOY_SSH_PRIVATE_KEY
```
-----BEGIN OPENSSH PRIVATE KEY-----
b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
QyNTUxOQAAACCvhbSXW1OYmnlVD4TdRPHBmuavKTQap7xKw2J4gol3BAAAAJiQH+ybkB/s
mwAAAAtzc2gtZWQyNTUxOQAAACCvhbSXW1OYmnlVD4TdRPHBmuavKTQap7xKw2J4gol3BA
AAAEDZzxHpihLvjrDVsq+wyqCJiPd5lYhPMScoRQtsTtaZiq+FtJdbU5iaeVUPhN1E8cGa
5q8pNBqnvErDYniCiXcEAAAAFWdpdGh1Yi1hY3Rpb25zLWRlcGxveQ==
-----END OPENSSH PRIVATE KEY-----
```

### DEPLOY_KNOWN_HOSTS
```
47.106.93.24 ecdsa-sha2-nistp256 AAAAE2VjZHNhLXNoYTItbmlzdHAyNTYAAAAIbmlzdHAyNTYAAABBBNhKMhdWNn1K9qdd8spJHR/5sRpTFwcF8TjsQmSAkSoX3HVngNOTCo9/dkFV92U+vBUOhAzs7NDru6qw7mkld6w=
47.106.93.24 ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDJpQ2zWGTqSZDDiU37xWFBM6pt2lI1oCHzarMDM1b4mx7/WR6LQeOz6HLao2Gjk9EOIYkbpBe4xDfoee+rkQaueQ43+7jsxlNlMXrFG1jhANZjZKrT0y7T01NMFIPC/fAu6ZpLg3pkzxgJD6ZlYayHWOz1yKFMfNZ7GoYXiDj1PEqMmh/Sw378faF+mRmuaPtUIx4z25iQu8lmghlacfLVi1rn9MPbb4+6EXQYSF235BIJZvKzRk3vzaGxCdbuLKpih2rdIUhvwfLMV5h2KIaq4rRxNWpZBb05MN+hLDb/vpVjabWUz3fr9VTIjH7N8TxT7vtdv0M6PAl269fK8+9J
47.106.93.24 ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIFh32k4Zv/FemLiKWgiL5yKxylS/1sawHzRW5Q55EEmb
```

### DEPLOY_HEALTHCHECK_URL
```
http://47.106.93.24/healthz
```

## 第四步: 配置服务器 SSH 公钥

SSH 登录服务器,将公钥添加到 authorized_keys:

```bash
ssh root@47.106.93.24

# 添加公钥
echo 'ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIK+FtJdbU5iaeVUPhN1E8cGa5q8pNBqnvErDYniCiXcE github-actions-deploy' >> ~/.ssh/authorized_keys

# 设置权限
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

## 第五步: 验证配置

在本地测试 SSH 连接:

```bash
ssh -i github-actions-deploy root@47.106.93.24 "echo 连接成功"
```

---

## 安全提示

**此文件包含敏感信息,请勿提交到 Git!**

配置完成后请删除此文件或将其保存在安全位置。
