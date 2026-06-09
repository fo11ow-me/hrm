# HRM - 人力资源管理系统

> 一个基于 Spring Boot + Vue.js 的全栈人力资源管理平台

## 📖 项目简介

HRM (Human Resource Management) 是一个功能完善的人力资源管理系统,提供员工管理、考勤管理、薪资管理、审批流程等核心 HR 业务功能。系统采用前后端分离架构,后端使用 Spring Boot + MyBatis-Plus,前端使用 Vue.js + Element UI,集成 Activiti 工作流引擎实现审批流程自动化。

## ✨ 核心功能

### 组织架构管理
- **员工管理**: 员工信息维护、头像上传、角色分配
- **部门管理**: 部门层级结构、部门负责人设置
- **菜单管理**: 动态菜单配置、权限控制

### 权限控制
- **角色管理**: RBAC 权限模型、角色菜单权限分配
- **认证机制**: JWT 双 Token 认证 (Access + Refresh Token)
- **安全防护**: httpOnly Cookie、密码加密、CSRF 防护

### 考勤与薪资
- **考勤管理**: 考勤记录、异常处理、数据导入导出
- **薪资管理**: 薪资计算、扣款管理、薪资条生成
- **绩效考核**: 考勤统计、绩效评估

### 审批流程
- **请假申请**: 集成 Activiti 工作流、多级审批
- **加班申请**: 审批流程自动化、审批记录追踪
- **流程监控**: 流程状态查询、审批历史查看

### 文档管理
- **文件上传**: MD5 去重、大小限制 (20MB)
- **文件下载**: 权限控制、路径穿越防护
- **数据导入导出**: Excel 导入导出、异步任务处理

### 员工自助服务
- **智能问答**: OpenAI 兼容的 LLM 适配器
- **自助查询**: 个人信息、薪资查询、考勤记录

## 🛠️ 技术栈

### 后端技术
- **框架**: Spring Boot 2.5.6
- **ORM**: MyBatis-Plus 3.5.1 (多数据源)
- **数据库**: MySQL 8.1
- **缓存**: Redis 5
- **认证**: Spring Security + JWT (jjwt 0.11.5)
- **工作流**: Activiti 7.0
- **工具库**: Hutool 5.8.25、EasyExcel 3.3.4
- **API 文档**: Swagger 2.9.2

### 前端技术
- **框架**: Vue.js 2.6.11
- **UI 组件**: Element UI 2.15.7
- **状态管理**: Vuex 3.6.2
- **路由**: Vue Router 3.2.0
- **HTTP 客户端**: Axios 0.25.0
- **图表**: ECharts 5.3.0
- **构建工具**: Vue CLI 4.5

### 开发环境
- **Java**: JDK 17
- **Node.js**: 14.x+
- **Maven**: 3.6+
- **Docker**: Docker Compose

## 📦 项目结构

```
hrm/
├── hrm-server/                # 后端项目
│   ├── src/main/java/com/qiujie/
│   │   ├── controller/        # 控制器层
│   │   ├── service/           # 服务层
│   │   ├── mapper/            # 数据访问层
│   │   ├── entity/            # 实体类
│   │   ├── dto/               # 数据传输对象
│   │   ├── vo/                # 视图对象
│   │   ├── config/            # 配置类
│   │   ├── filter/            # 过滤器
│   │   ├── enums/             # 枚举类
│   │   ├── assistant/         # LLM 适配器
│   │   └── listener/          # Activiti 监听器
│   ├── src/main/resources/
│   │   ├── application.yml         # 主配置
│   │   ├── application-dev.yml     # 开发环境配置
│   │   └── mapper/                 # MyBatis XML
│   └── src/test/              # 测试代码
├── hrm-admin/                 # 前端项目
│   ├── src/
│   │   ├── views/             # 页面组件
│   │   ├── components/        # 公共组件
│   │   ├── api/               # API 接口
│   │   ├── store/             # Vuex 状态
│   │   ├── router/            # 路由配置
│   │   ├── utils/             # 工具函数
│   │   └── assets/            # 静态资源
│   └── public/                # 公共资源
├── hrm.sql                    # 主数据库脚本
├── hrm_activiti.sql           # 工作流数据库脚本
├── assistant.sql              # LLM 审计表脚本 (可选)
└── docker-compose.yml         # 开发环境 Compose
```

## 🚀 快速开始

### 环境要求

- JDK 17+
- Node.js 14.x+
- MySQL 8.1+
- Redis 5+
- Maven 3.6+

### 1. 克隆项目

```bash
git clone https://github.com/fo11ow-me/hrm.git
cd hrm
```

### 2. 启动基础设施

```bash
# 启动 MySQL 和 Redis
docker-compose up -d

# 导入数据库脚本
mysql -h 127.0.0.1 -P 3306 -u root -p < hrm.sql
mysql -h 127.0.0.1 -P 3306 -u root -p < hrm_activiti.sql
```

### 3. 配置后端

创建 `hrm-server/src/main/resources/application-dev.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/hrm?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8
    username: root
    password: <数据库密码>
  redis:
    host: localhost
    port: 6379
    password: <Redis密码>

jwt:
  secret: <Base64编码的JWT密钥>

file-path: D:/hrm-files  # 文件存储路径
```

### 4. 启动后端

```bash
cd hrm-server
mvn spring-boot:run
```

后端服务将在 `http://localhost:8888` 启动,API 文档地址: `http://localhost:8888/swagger-ui.html`

### 5. 启动前端

```bash
cd hrm-admin
npm ci
npm run serve
```

前端服务将在 `http://localhost:8080` 启动,自动代理 `/api` 到后端。

### 6. 访问系统

打开浏览器访问 `http://localhost:8080`

**默认账号**:
- 用户名: `admin`
- 密码: `123`

## ⚙️ 配置说明

### 后端配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `server.port` | 服务端口 | 8888 |
| `spring.servlet.multipart.max-file-size` | 单文件大小限制 | 20MB |
| `spring.servlet.multipart.max-request-size` | 请求大小限制 | 30MB |
| `file-path` | 文件存储路径 | - |
| `jwt.secret` | JWT 密钥 (Base64) | - |
| `jwt.expiration` | Access Token 有效期 | 15分钟 |
| `jwt.refresh-expiration` | Refresh Token 有效期 | 7天 |

### 前端配置

编辑 `hrm-admin/.env`:

```env
VUE_APP_BACKEND_HOST='http://localhost'
VUE_APP_BACKEND_PORT=8888
VUE_APP_BASE_API='/api'
```

### LLM 配置 (可选)

启用员工自助问答功能:

```yaml
assistant:
  provider:
    base-url: <OpenAI兼容API地址>
    api-key: <API密钥>
    model: <模型名称>
```

## 🧪 测试

### 后端测试

```bash
cd hrm-server

# 运行所有测试
mvn test

# 仅运行单元测试
mvn test -Dtest=*UnitTest

# 仅运行集成测试
mvn test -Dtest=*IntegrationTest

# 跳过测试构建
mvn clean package -DskipTests
```

### 前端测试

```bash
cd hrm-admin
npm run lint
```

## 📚 API 文档

启动后端后访问 Swagger UI:

- 开发环境: `http://localhost:8888/swagger-ui.html`
- 生产环境: `http://<服务器地址>/swagger-ui.html`

## 🤝 开发指南

### 代码规范

- 遵循阿里巴巴 Java 开发手册
- 前端遵循 ESLint Standard 规范
- 提交信息遵循 Conventional Commits

### 分支管理

- `master`: 生产环境分支
- `dev`: 开发环境分支
- `feature/*`: 功能开发分支
- `hotfix/*`: 紧急修复分支

### 提交规范

```bash
feat: 新功能
fix: Bug 修复
docs: 文档更新
style: 代码格式调整
refactor: 重构
test: 测试相关
chore: 构建/工具链相关
```

## 📄 许可证

本项目采用 MIT 许可证,详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

感谢以下开源项目:
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [Element UI](https://element.eleme.io/)
- [Activiti](https://www.activiti.org/)
- [MyBatis-Plus](https://baomidou.com/)

---

**作者**: qiujie
**仓库**: https://github.com/fo11ow-me/hrm
