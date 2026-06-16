# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Reference Project

`D:\project\vscode\Argus` — 企业级 RAG 知识库平台（Vue 3 + Spring Boot 3.5 + PostgreSQL/pgvector + Elasticsearch + MinIO）。本项目的知识库模块（`com.qiujie.knowledge/`）可参照 Argus 的架构设计。

## Project Overview

HRM (Human Resource Management) system - a full-stack application with separate backend (Java/Spring Boot) and frontend (Vue.js) modules.

## Build & Run Commands

### Backend (hrm-server/)
```bash
cd hrm-server
mvn test                           # Run all tests (unit + integration)
mvn test -Dtest=*UnitTest          # Run only unit tests
mvn test -Dtest=*IntegrationTest   # Run only integration tests
mvn clean package -DskipTests      # Build JAR without tests
mvn spring-boot:run                # Run dev server (port 8888)
```

### Frontend (hrm-admin/)
```bash
cd hrm-admin
npm ci                             # Install dependencies
npm run serve                      # Dev server (proxies /api to backend)
npm run build                      # Production build
npm run lint                       # ESLint check
```

### Infrastructure
```bash
docker-compose up -d               # Start MySQL 8.1, Redis 5
```

Required services: MySQL (3306), Redis (6379). Import schema from `hrm.sql` and `hrm_activiti.sql`.

## Architecture

### Backend (Spring Boot 2.5.6, Java 17)
- **Package**: `com.qiujie`
- **ORM**: MyBatis-Plus with multi-datasource (master DB for app, separate DB for Activiti workflow engine)
- **Auth**: Spring Security + JWT with httpOnly Cookie-based dual-token refresh (Access + Refresh Token)
- **Layers**: Controller → Service → Mapper (extends BaseMapper)
- **Response format**: `ResponseDTO` via `Response.success()`/`Response.error()` helper
- **File uploads**: stored to configurable `file-path` directory
- **Workflow**: Activiti 7.0 for leave/overtime approval processes
- **Config**: `application.yml` (common) + `application-{profile}.yml` (profile-specific)
- **Key packages**:
  - `assistant/` - OpenAI-compatible LLM adapter for employee self-service Q&A
  - `entity/` - Domain objects (Staff, Dept, Role, Menu, Salary, Attendance, Leave, Overtime, etc.)
  - `enums/` - MyBatis-Plus enums implementing `BaseEnum`, auto-registered via `type-enums-package`
  - `filter/` - `JwtAuthenticationFilter` runs before Spring Security's auth filter
  - `listener/` - Activiti task listeners for approval workflows
  - `config/` - Security, Redis, MyBatis-Plus, Swagger, Holiday configs

### Frontend (Vue 2.6, Element UI 2.15)
- **Build**: Vue CLI 4.5
- **State**: Vuex with modules (staff, menu, permission, token, tag)
- **Routing**: Dynamic routes loaded from backend menu data; static route only for `/login`
- **API layer**: `/src/api/` - one file per resource, uses axios instance from `/src/utils/request.js`
- **Auth flow**: httpOnly Cookie-based dual-token mechanism; Access Token in Cookie with Path=/; Refresh Token in Cookie with Path=/refresh; automatic silent refresh on 401/1200; concurrent request queueing during refresh
- **Proxy**: `/api` prefix → backend (configured in `vue.config.js` via env vars)
- **Permissions**: Custom directive `v-permission` checks against permission store

### Key Domain Entities
- **Staff** - employee with role assignments (StaffRole) and menu permissions (via RoleMenu)
- **Leave/Overtime** - approval workflows processed through Activiti
- **Salary/SalaryDeduct** - payroll with deduction types
- **Attendance** - attendance tracking with status enums
- **Menu/Role** - RBAC permission system, menus define both navigation and route components
- **Assistant** - employee self-service Q&A; read-only, current-user scoped, backed by allowlisted service/mapper tools

## Database
- Two MySQL databases: `hrm` (app data) and `hrm_activiti` (workflow engine)
- Schema files: `hrm.sql` (all app tables, including assistant + file_task), `hrm_activiti.sql` (workflow engine)
- MyBatis-Plus mapper XML locations: `classpath:mapper/*.xml`

## Testing
- Unit tests: `*UnitTest.java` (run via maven-surefire-plugin)
- Integration tests: `*IntegrationTest.java` (run via maven-failsafe-plugin)
- CI requires secrets: `CI_DB_PASSWORD`, `CI_JWT_SECRET`

## Development Notes
- Swagger UI available at `/swagger-ui.html` when running
- Backend uses `@EnableScheduling` for scheduled tasks
- File upload max: 20MB per file, 30MB per request
- Frontend `npm run serve` requires `NODE_OPTIONS=--openssl-legacy-provider` (already configured in scripts)
- Assistant LLM config is optional. Set `ASSISTANT_PROVIDER_BASE_URL`, `ASSISTANT_PROVIDER_API_KEY`, and `ASSISTANT_PROVIDER_MODEL` to enable OpenAI-compatible chat completions.
- Production deployment via Docker Compose with manual image build and deployment
