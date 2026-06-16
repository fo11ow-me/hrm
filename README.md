# 数智人事

全栈人力资源管理系统，集成 RAG 知识库与 AI 对话助手。后端 Spring Boot 3.4 + 前端 Vue 2.6。

## 线上访问

https://qiujie.net.cn

默认账号：admin / 123（其他账号密码均为 123）

## 功能模块

| 模块 | 功能 |
|------|------|
| **系统管理** | 员工管理、部门管理、文件管理 |
| **权限管理** | 角色管理（分配菜单权限）、菜单管理（树形权限点） |
| **考勤管理** | 请假审批（Flowable 工作流）、考勤表现、加班详情 |
| **财务管理** | 薪资管理、五险一金、参保城市 |
| **首页仪表盘** | 统计卡片、考勤日历、ECharts 图表 |
| **知识库** | 文档上传（PDF/DOCX/MD/TXT）→ ETL 自动分块 → 向量化存储 → RAG 问答 |
| **AI 智能助手** | 多轮对话、Tool 调用（查请假/查考勤/查调休/查部门）、知识库检索 |

## 技术栈

### 后端

| 技术 | 版本 |
|------|------|
| Spring Boot | 3.4.4 |
| JDK | 17 |
| MyBatis-Plus | 3.5.10 |
| Spring Security + JWT | httpOnly Cookie 双 Token |
| Flowable | 8.0.0（工作流引擎） |
| Spring AI | 1.0.0-M6（Ollama 集成） |
| PostgreSQL + pgvector | 16（知识库向量存储） |
| MinIO | 对象存储 |
| Redis | 5 |
| MySQL | 8.1 |
| Ollama | 本地 LLM（nomic-embed-text + minimax-m3） |

### 前端

| 技术 | 版本 |
|------|------|
| Vue | 2.6.14 |
| Element UI | 2.15.7 |
| Vuex + Vue Router | 3.x |
| ECharts | 5.3.0 |
| Axios | 0.25.0 |

## 开发环境

### 依赖服务

```bash
# 启动所有容器
docker start hrm-mysql hrm-redis hrm-minio hrm-postgres

# 容器端口
# hrm-mysql:     3306 (hrm + hrm_flowable 数据库)
# hrm-redis:     6381 (密码: 123456)
# hrm-minio:     9000 (minioadmin / minioadmin)
# hrm-postgres:  5432 (hrm_kb 数据库, postgres / postgres)
# Ollama:        11434 (需手动安装启动)
```

### 后端启动

```bash
cd hrm-server
mvn spring-boot:run -D"spring-boot.run.arguments=--spring.profiles.active=dev --server.port=8889"
```

### 前端启动

```bash
cd hrm-admin
npm ci
npm run serve    # 端口 8080，/api 代理到 localhost:8889
```

### 知识库与 AI 助手

知识库和 AI 助手模块默认关闭，通过环境变量启用：

```bash
KNOWLEDGE_ENABLED=true              # 开启知识库
ASSISTANT_ENABLED=true              # 开启 AI 助手
```

启用前需确保：
- PostgreSQL + pgvector 扩展已安装
- Ollama 已安装 `nomic-embed-text` 和对话模型
- `sql/knowledge_base.sql` 中 MySQL 部分已执行

## 部署

生产环境通过 Docker Compose 部署，配置见 `deploy/` 目录。

## 项目文档

- 详细变更日志见 `tmp/其它.md`
<!-- 架构参照 Argus 项目（企业级 RAG 知识库平台），项目路径为私有信息不在此公开 -->
