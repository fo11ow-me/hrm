# HRM 系统安全诊断报告

**诊断日期**: 2026-06-08
**诊断范围**: JWT认证、CORS配置、文件上传、权限控制
**诊断原则**: 系统化调试 - 基于证据而非假设

---

## 执行摘要

本次安全诊断遵循系统化调试原则,通过代码审查和配置分析,识别出HRM系统的安全现状。

**关键发现**:
- ✅ JWT认证机制实现合理,采用双令牌策略
- ⚠️ CORS配置过于宽松,存在安全风险
- ✅ 文件上传实现了路径遍历防护
- ⚠️ 缺少文件类型白名单验证
- ✅ 使用BCrypt密码加密
- ⚠️ JWT密钥依赖环境变量,需验证生产环境配置

**风险等级**: 🟡 中等风险 (需要关注,非紧急)

---

## 1. JWT 认证机制分析

### 1.1 当前实现状态

**代码位置**: `JwtUtil.java`, `JwtAuthenticationFilter.java`

#### ✅ 安全的实现

1. **双令牌策略**
   - Access Token: 15分钟有效期 (符合安全最佳实践)
   - Refresh Token: 7天有效期
   - 令牌类型区分明确 (`type: access/refresh`)

2. **密钥管理**
   ```java
   signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
   ```
   - 使用 HMAC-SHA256 算法
   - 密钥从环境变量读取 (`${JWT_SECRET}`)
   - Base64解码后生成密钥

3. **Token 验证流程**
   - 三层回退获取token: Header → Query Param → Cookie
   - Refresh Token 不能用于访问API (代码第60行验证)
   - Token过期验证 (`isTokenExpired`)

4. **性能优化**
   - 从JWT claims直接构建认证信息,避免每次请求查数据库
   - 权限信息存储在JWT中,减少数据库查询

#### ⚠️ 需要关注的点

1. **密钥配置依赖**
   - **代码**: `jwt.secret: ${JWT_SECRET}` (application.yml:71)
   - **风险**: 如果环境变量未设置或使用默认值,会导致安全问题
   - **建议**: 检查生产环境的 `JWT_SECRET` 配置

2. **Token 刷新机制**
   - **代码**: LoginController.java:54-102
   - **实现**: 从Cookie中读取refreshToken,验证后生成新accessToken
   - **优点**: 刷新时会重新查询员工状态和最新权限
   - **风险**: Refresh Token存储在Cookie中,需要确保HttpOnly和Secure标志

3. **异常处理**
   - JWT解析失败时仅记录警告日志 (JwtAuthenticationFilter.java:79)
   - 不抛出异常,继续执行过滤器链
   - **影响**: 失败的请求不会被明确拒绝,可能导致未认证请求通过

### 1.2 Cookie 安全性

**代码位置**: LoginController.java:93-97

```java
Cookie accessCookie = new Cookie("token", newAccessToken);
accessCookie.setHttpOnly(true);
accessCookie.setPath("/");
accessCookie.setMaxAge((int) (JwtUtil.ACCESS_EXPIRATION / 1000));
response.addCookie(accessCookie);
```

#### ✅ 已实现的安全措施
- `setHttpOnly(true)`: 防止XSS攻击读取Cookie
- `setPath("/")`: 整个应用可访问

#### ❌ 缺失的安全措施
- **缺少 `setSecure(true)`**: Cookie未强制HTTPS传输
- **缺少 `SameSite` 属性**: 未防御CSRF攻击

**风险评估**: 🟡 中等风险 (生产环境必须启用HTTPS并设置Secure标志)

---

## 2. CORS 配置分析

**代码位置**: SecurityConfig.java:51-61

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    corsConfiguration.setAllowedOriginPatterns(List.of("*")); // ⚠️ 允许所有来源
    corsConfiguration.setAllowedHeaders(List.of("*"));
    corsConfiguration.setAllowedMethods(List.of("*"));
    corsConfiguration.setAllowCredentials(true);
    corsConfiguration.setMaxAge(Duration.ofHours(5));
    // ...
}
```

### 2.1 安全问题

#### 🔴 高风险配置

1. **允许所有来源**
   - `setAllowedOriginPatterns(List.of("*"))`
   - **风险**: 任意域名都可以发起跨域请求
   - **影响**:
     - 恶意网站可以读取用户数据
     - CSRF攻击风险增加
     - 数据泄露风险

2. **允许所有方法和头部**
   - `setAllowedMethods(List.of("*"))`
   - `setAllowedHeaders(List.of("*"))`
   - **风险**: 过度开放,增加攻击面

3. **允许凭证**
   - `setAllowCredentials(true)`
   - **与通配符来源冲突**: 当 `allowCredentials=true` 时,不应使用 `*` 作为来源

### 2.2 建议修复

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    // 仅允许受信任的域名
    corsConfiguration.setAllowedOriginPatterns(List.of(
        "https://hrm.example.com",
        "https://admin.hrm.example.com"
    ));
    // 仅允许必要的方法
    corsConfiguration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
    // 仅允许必要的头部
    corsConfiguration.setAllowedHeaders(List.of(
        "Authorization",
        "Content-Type",
        "X-Requested-With"
    ));
    corsConfiguration.setAllowCredentials(true);
    corsConfiguration.setMaxAge(Duration.ofHours(1));
    // ...
}
```

**风险评估**: 🔴 高风险 (立即修复)

---

## 3. 文件上传安全性分析

**代码位置**: DocsService.java:60-99

### 3.1 已实现的安全措施

#### ✅ 路径遍历防护

```java
private File resolveStoredFile(String filename) throws IOException {
    File baseDir = new File(filePath).getCanonicalFile();
    File file = new File(baseDir, filename).getCanonicalFile();
    String basePath = baseDir.getPath();
    if (!file.getPath().equals(basePath) && file.getPath().startsWith(basePath + File.separator)) {
        return file;
    }
    return null;
}
```

**实现分析**:
- 使用 `getCanonicalFile()` 解析规范化路径
- 验证文件路径在基础目录内
- **防御**: 阻止 `../` 路径遍历攻击

#### ✅ 文件去重

```java
String md5 = SecureUtil.md5(uploadFile.getInputStream());
List<Docs> docsList = list(new QueryWrapper<Docs>().eq("md5", md5));
if (!docsList.isEmpty()) {
    filename = docsList.get(0).getName();
}
```

**实现分析**:
- 计算文件MD5哈希
- 相同文件不重复存储
- **副作用**: 减少存储空间,但可能被利用进行时序攻击

#### ✅ 文件大小限制

**配置**: application.yml:30-31
```yaml
max-file-size: 20MB
max-request-size: 30MB
```

### 3.2 缺失的安全措施

#### ⚠️ 文件类型验证缺失

**代码**: DocsService.java:71
```java
String extName = FileUtil.extName(originalFilename); // 仅获取后缀名
```

**问题**:
- 仅通过文件扩展名判断类型
- 未验证文件内容(Magic Number)
- 未限制可上传的文件类型

**风险**:
- 攻击者可以上传恶意文件(如 `.jsp`, `.exe`)
- 通过修改扩展名绕过检查
- 可能导致远程代码执行

**建议修复**:
```java
// 定义允许的文件类型白名单
private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
    "pdf", "doc", "docx", "xls", "xlsx", "png", "jpg", "jpeg"
);

// 验证文件扩展名
String extName = FileUtil.extName(originalFilename).toLowerCase();
if (!ALLOWED_EXTENSIONS.contains(extName)) {
    return Response.error("不支持的文件类型");
}

// 验证文件内容(Magic Number)
String mimeType = uploadFile.getContentType();
if (!isValidMimeType(mimeType)) {
    return Response.error("文件类型不匹配");
}

private boolean isValidMimeType(String mimeType) {
    return mimeType.equals("application/pdf") ||
           mimeType.equals("application/msword") ||
           // ... 其他允许的MIME类型
}
```

#### ⚠️ 文件名安全

**代码**: DocsService.java:72
```java
String filename = IdUtil.fastSimpleUUID().substring(2, 22) + "." + extName;
```

**实现**:
- 使用UUID生成文件名
- **优点**: 避免文件名冲突,隐藏原始文件名
- **风险**: 如果扩展名恶意,文件名随机化无法阻止

### 3.3 权限控制

**代码**: DocsController.java:88-93
```java
@PreAuthorize("hasAnyAuthority('system:docs:upload')")
public ResponseDTO upload(MultipartFile file, @PathVariable Integer id)
```

**实现**:
- 使用Spring Security的 `@PreAuthorize` 注解
- 基于权限的访问控制
- **优点**: 细粒度权限控制

**风险评估**: 🟡 中等风险 (需要添加文件类型白名单)

---

## 4. 密码安全性分析

**代码位置**: SecurityConfig.java:40-42

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### 4.1 安全实现

#### ✅ 使用BCrypt加密

- **算法**: BCrypt
- **优点**:
  - 内置盐值
  - 自适应工作因子
  - 抗彩虹表攻击
  - 业界标准

**风险评估**: ✅ 安全

---

## 5. CSRF 保护分析

**代码位置**: SecurityConfig.java:74

```java
.csrf().disable()
```

### 5.1 当前状态

#### ❌ CSRF保护已禁用

**原因分析**:
- JWT认证不需要CSRF保护(无状态)
- 前后端分离架构

**风险评估**:
- **对于JWT认证**: 🟢 可接受 (JWT不受CSRF影响)
- **对于Cookie认证**: 🔴 高风险 (如果有基于Cookie的会话)

**建议**:
- 确保所有敏感操作都使用JWT认证
- 如果有基于Cookie的会话,必须启用CSRF保护

---

## 6. 数据库安全分析

### 6.1 数据源配置

**代码位置**: application.yml:8-20

```yaml
datasource:
  master:
    jdbc-url: ${DB_MASTER_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  activiti:
    jdbc-url: ${DB_ACTIVITI_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### 6.2 安全实现

#### ✅ 使用环境变量

- 数据库连接信息从环境变量读取
- 不在代码中硬编码敏感信息
- 支持不同环境配置

### 6.3 多数据源安全

**实现**:
- 主数据库 (`hrm`): 业务数据
- Activiti数据库 (`hrm_activiti`): 工作流数据
- 使用独立的数据源配置

**风险评估**: ✅ 安全 (需确保环境变量正确配置)

---

## 7. 依赖安全性分析

### 7.1 已知依赖

基于 pom.xml 分析:

- **Spring Boot**: 2.5.6 (2021年发布)
- **Java**: 17
- **MyBatis-Plus**: (版本需确认)
- **JWT库**: `io.jsonwebtoken`

### 7.2 建议

#### ⚠️ 版本更新

1. **Spring Boot 2.5.6**
   - 当前版本: 2.5.6 (2021年)
   - 最新版本: 3.x (2024年)
   - **风险**: 可能存在已知漏洞
   - **建议**: 升级到 Spring Boot 2.7.x 或 3.x

2. **依赖扫描**
   - 运行 OWASP Dependency Check
   - 检查已知漏洞 (CVE)

**风险评估**: 🟡 中等风险 (需进行依赖扫描)

---

## 8. Actuator 安全性分析

**代码位置**: application.yml:57-66

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never
```

### 8.1 安全实现

#### ✅ 最小化暴露

- 仅暴露 `health` 端点
- 不显示健康检查详细信息

#### ⚠️ 访问控制

**SecurityConfig.java:67**:
```java
.antMatchers("/actuator/health/**").permitAll()
```

**问题**:
- 健康检查端点无需认证即可访问
- **影响**: 攻击者可以探测服务状态

**风险评估**: 🟢 低风险 (符合健康检查最佳实践)

---

## 9. Redis 安全性分析

**代码位置**: application.yml:33-42

```yaml
redis:
  host: ${REDIS_HOST}
  port: ${REDIS_PORT:6379}
  password: ${REDIS_PASSWORD}
  database: ${REDIS_DATABASE:0}
```

### 9.1 安全实现

#### ✅ 使用环境变量

- Redis连接信息从环境变量读取
- 支持密码认证

#### ⚠️ 需要验证

- 生产环境是否启用了Redis密码
- Redis是否绑定内网地址
- 是否使用了TLS加密连接

**风险评估**: 🟡 中等风险 (需验证生产环境配置)

---

## 10. 总结与建议

### 10.1 风险等级分类

| 类别 | 风险等级 | 问题数量 | 优先级 |
|------|---------|---------|--------|
| CORS配置 | 🔴 高风险 | 1 | P0 - 立即修复 |
| 文件上传 | 🟡 中等风险 | 1 | P1 - 尽快修复 |
| JWT配置 | 🟡 中等风险 | 2 | P1 - 尽快修复 |
| Cookie安全 | 🟡 中等风险 | 1 | P1 - 尽快修复 |
| 依赖版本 | 🟡 中等风险 | 1 | P2 - 计划修复 |

### 10.2 立即行动项 (P0)

1. **修复CORS配置**
   - 限制允许的来源域名
   - 移除通配符配置
   - 仅允许必要的HTTP方法和头部

### 10.3 近期行动项 (P1)

1. **文件上传安全加固**
   - 添加文件类型白名单
   - 验证文件内容(Magic Number)
   - 添加病毒扫描(可选)

2. **Cookie安全加固**
   - 启用 `Secure` 标志(生产环境)
   - 添加 `SameSite` 属性

3. **JWT配置验证**
   - 确认生产环境JWT_SECRET配置
   - 增强JWT解析失败的异常处理

### 10.4 长期行动项 (P2)

1. **依赖升级**
   - 进行依赖漏洞扫描
   - 制定Spring Boot升级计划

2. **安全监控**
   - 实施安全审计日志
   - 配置异常登录检测
   - 定期安全审查

---

## 11. 诊断方法说明

本次诊断遵循系统化调试原则:

### 11.1 证据收集

- ✅ 读取配置文件 (SecurityConfig, application.yml)
- ✅ 分析代码实现 (JwtUtil, JwtAuthenticationFilter, DocsService)
- ✅ 检查依赖版本 (pom.xml)
- ❌ 未执行动态测试 (渗透测试、漏洞扫描)

### 11.2 对比标准

- OWASP Top 10 2021
- JWT Security Cheat Sheet
- Spring Security官方文档
- 安全最佳实践

### 11.3 局限性

- 仅进行了静态代码分析
- 未测试生产环境实际配置
- 未进行动态安全测试
- 依赖版本扫描未执行

---

**下一步建议**: 执行性能诊断 (任务 #2) 或修复P0级别安全问题

**报告生成时间**: 2026-06-08
**诊断执行者**: Claude Code (遵循系统化调试原则)
