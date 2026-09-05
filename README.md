# 数智人事 (HRM System)

> 基于 Spring Boot 3.4 与 Vue 2.6 构建的现代化全栈人力资源管理系统，深度集成 Flowable 工作流引擎、RAG 知识库与大语言模型（LLM）智能员工助手。

---

## 📖 项目介绍

**数智人事** 是一套面向现代企业的高效人力资源全流程管理系统，旨在提升组织人效与员工自主服务体验。系统采用前后端分离架构，融合企业业务管理规范与先进的生成式 AI 技术：

- **传统业务闭环**：覆盖组织架构、员工全生命周期档案、菜单与角色细粒度 RBAC 权限体系、Flowable 审批流转、灵活考勤与打卡统计、精细化加班与多维假期管理、薪资核算与五险一金配置。
- **智能化升级**：提供基于 RAG（检索增强生成）的企业知识库问答以及具备员工上下文感知的智能 AI 助手，支持员工自助查询假期余额、考勤异常、组织架构等高频人事事务。

---

## ✨ 核心亮点

- **业务流程深度解耦与纯计算领域设计**：
  - 考勤核算、加班计算（`OvertimeCalculator`）、审批副作用处理（`LeaveApprovalSideEffects`）以及薪资核算（`SalaryCalculation`）全面收敛为无框架依赖的纯函数领域模型，具备极高的单测覆盖与维护确定性。
- **Flowable 8.0 流程引擎原生适配**：
  - 深度整合 Spring Boot 3.4 与 Flowable 引擎，基于双 MySQL 独立数据源设计（业务主库 + 流程引擎专库），保证核心工作流数据自治与平滑伸缩。
- **双 Token 自动无感续期机制**：
  - 基于 httpOnly Cookie 的双 Token（Access Token + Refresh Token）鉴权体系，前端拦截器透明处理并发无感刷新，阻断 XSS 与 Token 劫持风险。
- **混合持久化与企业级 RAG 知识检索**：
  - 采用 MySQL（业务）+ Redis（缓存/验证码）+ PostgreSQL/pgvector（向量数据库）+ MinIO（对象存储）的多引擎存储架构；内置文档切块、向量化 ETL 流水线，助力企业政策与制度的高精度语义召回。
- **企业员工 AI 自助助手**：
  - 搭载兼容 DashScope / OpenAI 规范的智能问答助手，依托 Tool Calling 动态查询当前员工上下文（考勤、调休余额、部门分布等），实现“即问即查”。

---

## 🖼️ 页面展示

系统各主要功能模块页面效果如下：

### 1. 登录认证与首页仪表盘
| 登录界面 | 首页仪表盘（工作台与考勤日历） |
| :---: | :---: |
| ![登录界面](docs/screenshots/01-login.png) | ![首页仪表盘](docs/screenshots/02-dashboard.png) |

### 2. 系统管理
| 员工管理 | 部门管理 |
| :---: | :---: |
| ![员工管理](docs/screenshots/03-staff-management.png) | ![部门管理](docs/screenshots/04-dept-management.png) |
| **文件管理** | |
| ![文件管理](docs/screenshots/05-file-management.png) | |

### 3. 权限管理
| 角色权限分配 | 菜单权限配置 |
| :---: | :---: |
| ![角色管理](docs/screenshots/06-role-permission.png) | ![菜单管理](docs/screenshots/07-menu-management.png) |

### 4. 考勤与审批管理
| 请假申请与审批 | 考勤表现分析 |
| :---: | :---: |
| ![请假审批](docs/screenshots/08-leave-approval.png) | ![考勤表现](docs/screenshots/09-attendance-record.png) |
| **加班详情核算** | |
| ![加班详情](docs/screenshots/10-overtime-detail.png) | |

### 5. 财务与社保管理
| 员工薪资管理 | 参保城市与五险一金比例 |
| :---: | :---: |
| ![薪资管理](docs/screenshots/11-salary-management.png) | ![参保城市](docs/screenshots/12-social-insurance.png) |

### 6. RAG 知识库与智能助手
| 知识库文档管理与分块 | 智能人事问答助手 |
| :---: | :---: |
| ![知识库管理](docs/screenshots/13-knowledge-base.png) | ![AI 智能助手](docs/screenshots/14-ai-assistant.png) |

---

## 🔄 核心业务流程图

系统关键核心业务（请假/加班审批流转链路 + 企业 RAG 智能问答闭环）如下所示：

```mermaid
flowchart TD
    subgraph ApprovalFlow["1. 请假与加班审批流转链路"]
        A1[员工端发起申请] --> A2{是否满足申请校验}
        A2 -- 余额/天数合法 --> A3[写入业务申请表并启动 Flowable 流程]
        A2 -- 校验不通过 --> A1
        A3 --> A4[主管/审批人接收任务]
        A4 --> A5{审批决策}
        A5 -- 驳回 --> A6[更新业务单据状态为 REJECT]
        A5 -- 批准 --> A7[触发 LeaveApprovalSideEffects 副作用链]
        A7 --> A8[扣减员工请假天数 / 增加调休时长]
        A8 --> A9[写入考勤状态记录与站内通知]
    end

    subgraph RagFlow["2. RAG 知识库与 AI 助手问答链路"]
        B1[管理员上传制度文档] --> B2[MinIO 原始文件存储]
        B2 --> B3[ETL 管道: 文档解析与切块]
        B3 --> B4[DashScope Embedding 向量化]
        B4 --> B5[(PostgreSQL pgvector 向量库)]
        
        C1[员工发送人事咨询问题] --> C2[AI 助手路由与意图识别]
        C2 -- 自助事务查询 --> C3[Tool Calling: 查调休/查考勤/查部门]
        C2 -- 政策制度咨询 --> C4[语义向量相似度检索]
        C4 --> B5
        B5 --> C5[召回相关文档切块上下文]
        C3 --> C6[组合上下文与 Prompt]
        C5 --> C6
        C6 --> C7[大模型生成回答 qwen3.7-flash]
        C7 --> C8[流式/同步反馈给员工前端]
    end
```

---

## 🛠️ 技术栈

### 后端核心架构

| 技术组件 | 版本 | 用途与说明 |
| :--- | :--- | :--- |
| **Java** | 17 | 开发语言运行环境 |
| **Spring Boot** | 3.4.4 | 现代化微服务脚手架与依赖管理 |
| **MyBatis-Plus** | 3.5.10 | ORM 增强框架，支持多数据源隔离与灵活查询 |
| **Flowable** | 8.0.0 | 原生支持 Spring Boot 3 的企业级工作流引擎 |
| **Spring AI** | 1.0.0-M6 | 大模型抽象层，驱动 LLM 对话与 Embedding 交互 |
| **Spring Security** | 6.x | 细粒度 RBAC 安全认证与授权控制 |
| **JJWT** | 0.11.5 | 无状态的双 Token 签名、加解密与有效期校验 |
| **SpringDoc OpenAPI**| 2.8.5 | 遵循 OpenAPI 3.1 规范的接口交互文档（Swagger-UI） |
| **MySQL** | 8.1 | 主业务数据库（`hrm`）与流程引擎专库（`hrm_flowable`） |
| **PostgreSQL** | 16 (pgvector) | 知识库文档分块与高维向量存储数据库（`hrm_kb`） |
| **Redis** | 5.0 | 分布式会话缓存、图形验证码验证与热点数据加速 |
| **MinIO** | Latest | S3 兼容的高性能私有化对象存储服务 |

### 前端技术栈

| 技术组件 | 版本 | 用途与说明 |
| :--- | :--- | :--- |
| **Vue.js** | 2.6.14 | 核心渐进式前端渲染框架 |
| **Element UI** | 2.15.7 | 经典桌面端企业级 UI 组件库 |
| **Vue Router** | 3.2.0 | 前端路由管理，支持基于后端动态菜单的路由载入 |
| **Vuex** | 3.6.2 | 全局集中式状态管理（用户信息、权限点列表等） |
| **Axios** | 0.25.0 | 网络请求库，封装响应拦截器以支持 401 自动无感静默刷新 |
| **ECharts** | 5.3.0 | 首页仪表盘考勤分布与可视化图表分析 |

---

## 🏗️ 系统架构图

```mermaid
graph TB
    subgraph Client["前端展现层 (hrm-admin)"]
        UI[Vue 2.6 + Element UI]
        Router[动态路由 / 权限守卫]
        Vuex[全局状态管理]
        AxiosInter[Axios 拦截器: 双 Token 无感续期]
    end

    subgraph Security["安全控制与网关切面"]
        Filter[JwtAuthenticationFilter: Cookie 解析与认证]
        PermChecker[Spring Security: RBAC 角色/权限检查]
    end

    subgraph CoreBackend["后端核心业务服务 (hrm-server)"]
        subgraph Domains["纯计算/高内聚领域模块"]
            Overtime[OvertimeCalculator<br/>加班时长核算]
            Salary[SalaryCalculation<br/>薪资社保核算]
            LeaveSide[LeaveApprovalSideEffects<br/>审批副作用流转]
        end

        subgraph Modules["应用服务层"]
            StaffSvc[员工/部门管理服务]
            AttendanceSvc[考勤与打卡分析服务]
            FlowEngine[Flowable 8.0 工作流引擎]
            KnowledgeSvc[RAG IngestionPipeline 知识切块]
            AiAssistantSvc[AI Assistant 智能助手与工具分发]
        end
    end

    subgraph ExternalModel["大模型云端服务"]
        LLM[DashScope: qwen3.7-flash 对话]
        Embed[DashScope: qwen3.7-text-embedding 向量嵌入]
    end

    subgraph Persistence["多引擎存储层"]
        DB_Master[(MySQL: hrm 业务主库)]
        DB_Flowable[(MySQL: hrm_flowable 流程专库)]
        DB_Vector[(PostgreSQL + pgvector: 向量库)]
        Cache_Redis[(Redis: 缓存/验证码)]
        Storage_MinIO[(MinIO: 附件与制度文档存储)]
    end

    Client -->|HTTP/REST /api| Filter
    Filter --> PermChecker
    PermChecker --> Modules
    Modules --> Domains

    StaffSvc --> DB_Master
    AttendanceSvc --> DB_Master
    Salary --> DB_Master
    FlowEngine --> DB_Flowable
    
    KnowledgeSvc --> Storage_MinIO
    KnowledgeSvc --> Embed
    KnowledgeSvc --> DB_Vector

    AiAssistantSvc --> LLM
    AiAssistantSvc -.检索召回.-> DB_Vector
    AiAssistantSvc -.Tool 调用.-> StaffSvc

    Filter -.验证码与黑名单.-> Cache_Redis
```

---

## 📂 项目目录结构

```text
hrm/
├── docker-compose.yml              # 本地中间件一键启动编排 (MySQL, Redis, PostgreSQL, MinIO)
├── sql/                            # 数据库初始化脚本
│   └── schema/
│       ├── mysql/
│       │   ├── hrm.sql             # 业务主库表结构与系统种子数据
│       │   └── hrm_flowable.sql    # Flowable 流程引擎专库表结构
│       └── postgresql/
│           └── knowledge_base.sql  # PostgreSQL + pgvector 知识库模式
├── docs/                           # 项目开发规格、测试报告及页面截图
│   └── screenshots/                # README 引用之核心页面截图
├── hrm-admin/                      # 前端工程 (Vue 2.6 + Element UI)
│   ├── public/                     # 页面模板与图标资源
│   └── src/
│       ├── api/                    # 资源粒度划分的后端接口模块
│       ├── assets/                 # 静态样式与全局图标
│       ├── components/             # 通用业务组件 (头部头像/通知、AI 助手抽屉)
│       ├── router/                 # 路由定义与权限动态加载
│       ├── store/                  # Vuex 模块 (staff, menu, token, permission)
│       ├── utils/                  # 请求封装、无感刷新、验证码与头像加载工具
│       └── views/                  # 业务功能页面
│           ├── home/               # 首页仪表盘与打卡日历
│           ├── system/             # 员工、部门、文件存储管理
│           ├── permission/         # 角色与菜单权限管理
│           ├── performance/        # 考勤打卡、加班明细、请假审批
│           ├── money/              # 薪资明细、五险一金比例与参保城市
│           └── knowledge/          # 知识库文档上传、解析与问答
└── hrm-server/                     # 后端工程 (Spring Boot 3.4 + Java 17)
    ├── src/main/java/com/qiujie/
    │   ├── HrmApplication.java     # 后端主入口启动类
    │   ├── assistant/              # AI 智能助手、会话上下文与 Tool Calling 适配
    │   ├── attendance/             # 考勤表现分析与批量打卡批处理
    │   ├── config/                 # 安全拦截、Redis、MyBatis 多数据源配置
    │   ├── controller/             # RESTful API 控制层
    │   ├── entity/                 # MyBatis-Plus 领域实体对象
    │   ├── filetask/               # 异步大文件导入/导出任务调度
    │   ├── filter/                 # JwtAuthenticationFilter 安全过滤
    │   ├── knowledge/              # RAG 知识管道 (切块、向量化、混合检索)
    │   ├── leaveapproval/          # 请假审批核心副作用端口与领域流转
    │   ├── mapper/                 # MyBatis 数据持久层 Mapper 接口
    │   ├── overtime/               # OvertimeCalculator 纯计算加班计算引擎
    │   ├── salarycalculation/      # SalaryCalculation 纯静态薪资核算核心
    │   ├── service/                # 综合业务服务实现层
    │   ├── storage/                # MinIO 对象存储适配实现
    │   └── util/                   # JWT 工具、DateTime 与验证码生成器
    └── src/main/resources/
        ├── application.yml         # 核心公共配置
        └── application-dev.yml     # 本地开发环境 Profiles
```

---

## 🚀 本地快速启动指南

### 1. 启动前置依赖容器
项目本地所需的全部中间件（MySQL 8.1、Redis 5.0、PostgreSQL/pgvector 16、MinIO）已通过根目录下的 `docker-compose.yml` 统一编排，无需在本机单独安装任何中间件：

```bash
# 在项目根目录下执行，一键启动所有容器
docker compose up -d
```

容器就绪后，默认连接信息如下（遵循统一本地开发约定）：
- **MySQL**：`localhost:3306`（用户 `root` / 密码 `123456`，包含 `hrm` 和 `hrm_flowable` 两个数据库）
- **Redis**：`localhost:6379`（无密码）
- **PostgreSQL (pgvector)**：`localhost:5432`（库名 `hrm_kb`，用户 `hrm` / 密码 `123456`）
- **MinIO**：API 端口 `9000`，控制台 `9001`（账号 `minioadmin` / 密码 `minioadmin`）

---

### 2. 导入数据库初始脚本
如果容器为首次启动（未挂载已有持久化卷），需依次执行数据库初始化脚本：

```bash
# 1. 导入 MySQL 业务库与 Flowable 流程引擎库
docker exec -i hrm-mysql mysql -uroot -p123456 hrm < sql/schema/mysql/hrm.sql
docker exec -i hrm-mysql mysql -uroot -p123456 hrm_flowable < sql/schema/mysql/hrm_flowable.sql

# 2. 导入 PostgreSQL 知识库与向量表
docker exec -i hrm-postgres psql -U hrm -d hrm_kb < sql/schema/postgresql/knowledge_base.sql
```

---

### 3. 配置本地开发环境参数
检查后端配置文件 `hrm-server/src/main/resources/application-dev.yml`：
- **AI 助手与知识库**：
  若需开启完整的 AI 智能助手或文档向量检索功能，请在 `application-dev.yml` 中配置你的阿里百炼 DashScope API Key（兼容 OpenAI 规范）：
  ```yaml
  spring:
    ai:
      dashscope:
        api-key: <YOUR_DASHSCOPE_API_KEY>
      openai:
        api-key: <YOUR_DASHSCOPE_API_KEY>
        base-url: https://dashscope.aliyuncs.com/compatible-mode
        chat:
          options:
            model: qwen3.7-flash
  knowledge:
    enabled: true
    embedding:
      api-key: <YOUR_DASHSCOPE_API_KEY>
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      model: qwen3.7-text-embedding
  ```

---

### 4. 启动后端服务
```bash
cd hrm-server

# 运行领域单测，验证环境
mvn test -Dtest=*UnitTest

# 启动 Spring Boot 后端服务（默认端口 8888）
mvn spring-boot:run
```
- 后端服务启动成功后，可在浏览器访问 OpenAPI 接口文档：`http://localhost:8888/swagger-ui.html`

---

### 5. 启动前端服务
```bash
cd hrm-admin

# 安装依赖
npm ci

# 启动前端开发服务器 (默认端口 8080)
npm run serve
```

---

### 6. 访问系统与默认凭据
- 打开浏览器访问：`http://localhost:8080`
- **默认管理员账号**：`admin`
- **默认密码**：`123456`（系统其他默认测试员工密码均为 `123`）
- 登录验证码：如需直接从 Redis 获取验证码，可在终端执行 `docker exec -it hrm-redis redis-cli get "validate:code"` 查看。

---

## 💬 交流与反馈

如果您在学习、部署或使用该项目的过程中遇到任何问题，欢迎加入技术交流群共同探讨：

- **QQ 交流群**：`967925576`

