# Getbrick 框架设计

> 状态：**待评审**。本文定稿后才开始写代码。

## 1. 定位

**业务基座层**，不是再造一个 Spring。目标是把每个业务项目里反复造轮子的那部分
——统一响应、异常、租户、数据权限、审计、幂等、限流——收敛成可复用的 starter。

判定标准：一个能力只有同时满足下面三条才进 Getbrick。

1. 每个业务项目都会写，且写法基本一样
2. 写错了会出安全问题或数据事故（租户串数据、审计字段漏填、幂等失效）
3. 能通过 starter 关闭，关闭后对业务代码零侵入

不满足就留在项目里。**宁可模块少，不要模块杂。**

## 2. 技术底座

| 项 | 选型 | 版本 | 说明 |
| --- | --- | --- | --- |
| JDK | Oracle JDK | 25 | Spring Boot 4 官方支持 |
| 框架 | Spring Boot | 4.1.1 | Spring Framework 7.0.9，Jakarta EE 11 / Servlet 6.1 |
| 持久层 | MyBatis-Plus | 3.5.17 | 用 `mybatis-plus-spring-boot4-starter` |
| 多数据源 | dynamic-datasource | 4.5.0 | 用 `-spring-boot4-starter` |
| API 文档 | springdoc-openapi | 3.1.1 | 3.x 对应 Boot 4 |
| 熔断限流 | resilience4j | 2.4.0 | 限流用 `RateLimiter`，不自写令牌桶 |
| 缓存 | Redisson | 4.7.0 | 分布式锁 / 幂等存储 |
| 工具库 | Hutool | 5.8.47 | 已有就不自己写 `StringUtils` |
| 序列化 | Jackson **3** | 由 Boot BOM 管 | `tools.jackson.*`，包名不再是 `com.fasterxml.*` |

### 2.1 Boot 4 的两个坑（已确认）

| 变化 | 旧 | 新 |
| --- | --- | --- |
| AOP starter | `spring-boot-starter-aop` | **`spring-boot-starter-aspectj`**（已移除旧的） |
| Jackson | `com.fasterxml.jackson:jackson-bom` 2.x | **`tools.jackson:jackson-bom` 3.x** |

Jackson 3 的 `groupId` 和包名都变了，代码里是 `tools.jackson.databind.ObjectMapper`。

## 3. 版本管理的归属划分

**Spring Boot 能管的，一律不自己管。** `getbrick-dependencies` 改为：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-dependencies</artifactId>
    <version>${spring-boot.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

由 Spring Boot BOM 接管的：Spring Framework、Jackson 3、JUnit 6、Hibernate Validator 9、
Testcontainers、Logback、SLF4J、Micrometer、Caffeine。

Getbrick BOM 只补 Spring Boot 不管的：`mybatis-plus-bom`、`dynamic-datasource`、
`redisson`、`hutool`、`resilience4j-bom`、`transmittable-thread-local`、`mapstruct`。

这样做的收益：不再出现「BOM 锁 Jackson 2、Boot 要 Jackson 3」这种正面冲突。
代价：Getbrick BOM 不能单独用，必须和 Spring Boot 一起进项目——本来就该如此。

## 4. 仓库划分

按能力域拆仓，每个仓库一个制品。依赖方向严格单向，三个 starter 互不依赖。

```
                 ┌─────────────────┐
                 │  getbrick-build │  父 POM + BOM
                 └────────┬────────┘
                          │
                 ┌────────▼────────┐
                 │  getbrick-core  │  契约层（库，无 starter）
                 └───┬──────┬───┬──┘
         ┌───────────┘      │   └───────────┐
   ┌─────▼──────┐    ┌──────▼──────┐  ┌─────▼───────┐
   │getbrick-web│    │getbrick-data│  │getbrick-    │
   │            │    │             │  │config       │
   └────────────┘    └─────────────┘  └─────────────┘
```

| 仓库 | 类型 | 依赖 | 职责 |
| --- | --- | --- | --- |
| `getbrick-core` | 库 | `spring-boot`、`spring-web`、`jakarta.validation` | 契约：响应信封、错误码、异常、分页模型、注解、上下文 |
| `getbrick-data` | starter | `core` + MyBatis-Plus + dynamic-datasource | 审计填充、租户隔离、数据权限、乐观锁、多数据源、分页装配 |
| `getbrick-web` | starter | `core` + `spring-boot-starter-web` + springdoc + resilience4j | 全局异常、校验增强、traceId、幂等切面、限流、OpenAPI |
| `getbrick-config` | starter | `core` | 配置绑定与启动校验、动态刷新 SPI、多环境约定 |

### 4.1 依赖规则

- starter 之间**不互相依赖**。`web` 不引用 `data`，`data` 不引用 `web`。
- `core` 不知道 MyBatis-Plus 存在。`PageResult` 定义在 core，`IPage` 的转换在 data。
  代价是 data 要写一个薄薄的转换器，收益是 core 保持零第三方依赖。
- 依赖方向由 `maven-enforcer-plugin` 的 `bannedDependencies` 在 CI 里卡住，不靠人盯 review。

## 5. 各仓库内容

### 5.1 getbrick-core

只有契约，没有实现。别人编译期能依赖它。

- `Result<T>` — 统一响应信封，成功与失败同一个形状
- `ErrorCode` — 错误码接口 + `ErrorCodeRegistry`
- `GetbrickException` / `GetbrickErrorCodeException` — 异常基类
- `PageResult<T>` — 分页模型（自己的类型，不引 `IPage`）
- 注解：`@Idempotent`、`@Audit`、`@TenantIgnore`、`@DataScope`、`@OperationLog`
  —— 只有标记，不含任何逻辑
- `RequestContext` — traceId / userId / tenantId 的持有接口
- `TenantContext` — 租户持有接口

**明确不做**：全局异常处理器（属 web）、工具类（Hutool）、JSON 处理（Jackson）、
校验注解（Jakarta Validation）、线程池（`TaskExecutor`）。

### 5.2 getbrick-data

全部靠 MyBatis-Plus 已有能力，只做「装配」和「注解 → 拦截器」的胶水。

- 审计字段自动填充 — `MetaObjectHandler` 填 `createdBy` / `createdAt` / `updatedBy` / `updatedAt`
- 租户隔离 — `TenantLineInnerInterceptor`，`@TenantIgnore` 作为开关旁路
- 数据权限 — `@DataScope` 注解 + `DataScopeInnerInterceptor` 改写 SQL
- 乐观锁 — `OptimisticLockerInnerInterceptor`
- 分页 — `PaginationInnerInterceptor` 装配 + `IPage` → `PageResult<T>` 转换器
- 多数据源 — dynamic-datasource 装配 + 数据源切换注解

**明确不做**：通用 CRUD 基类。项目之间口径差太多，最后一定变成垃圾场。

### 5.3 getbrick-web

- 全局异常处理 — `@RestControllerAdvice`，把异常转成 `Result`
- 参数校验增强 — 分组校验、错误信息聚合、错误路径提取
- 请求追踪 — 生成或透传 traceId，写 MDC + `X-Trace-Id` 响应头
- 幂等 — `@Idempotent` 切面，存储可插拔（Redis / 本地）
- 限流 — resilience4j `RateLimiter`
- OpenAPI — springdoc 装配 + 统一响应 schema

**明确不做**：鉴权与权限。用 Spring Security 或 sa-token，不自己写。
**明确不做**：全局异常里塞业务逻辑。异常处理器只做格式转换。

### 5.4 getbrick-config

- `@ConfigurationProperties` 绑定 + JSR-303 校验，不合法则启动失败
- 动态刷新：`@Refreshable` 注解 + `ConfigRefresher` **接口**，实现留给项目
- 多环境 profile 约定与配置项命名规范

**明确不做**：配置中心客户端。Nacos / Apollo / K8s ConfigMap 各家不同，做成 SPI 由项目提供实现。

## 6. 开关约定

每个能力都有独立开关，默认关闭还是开启要逐个定。统一前缀 `getbrick.*`：

```yaml
getbrick:
  data:
    audit:
      enabled: true
    tenant:
      enabled: false
    data-scope:
      enabled: false
  web:
    idempotent:
      enabled: true
      store: redis        # redis | local
    rate-limit:
      enabled: false
  config:
    refresh:
      enabled: false
```

所有 starter 用 `AutoConfiguration.imports` 注册，靠 `@ConditionalOnProperty` 控制。
关闭时不应引入任何 Bean，也不对业务代码产生编译期依赖。

## 7. 风险

| 风险 | 说明 | 应对 |
| --- | --- | --- |
| MyBatis-Plus 3.5.17 传递依赖被报 CVE-2026-41001 | 需要确认受影响范围 | 开工前先跑一次依赖漏洞扫描，评估是否需要排除或换版本 |
| Boot 4 生态仍在补齐 | 部分库只出到 Boot 3 | 已在第 2 节逐个确认过版本；后续依赖只进 BOM，不进业务仓库 |
| `spring-boot-starter-aop` 改名 | 旧名字在 Boot 4 不存在 | 已确认新名字，所有 AOP 依赖统一写 `spring-boot-starter-aspectj` |
| 多仓版本漂移 | 三个 starter 各自发版 | 全组织同版本号，BOM 统一对齐；`getbrick-core` 版本作为下限 |

## 8. 实施顺序

1. **改 `getbrick-dependencies`**：改成 import `spring-boot-dependencies`，补齐第 2 节里
   Boot 不管的依赖。这是所有后续工作的前置。
2. **起 `getbrick-core`**：纯契约，带完整单测。这是另外三个仓库的编译期依赖，必须先有。
3. **起 `getbrick-web`**：最能独立验证编译期依赖方向，选它做第二个。
4. **起 `getbrick-data`**：依赖 MP，验证成本最高，放最后。
5. `getbrick-config` 最后，内容最少。

每个仓库：pom → CI → 契约类 + 单测 → 实现，开箱即可合并。
