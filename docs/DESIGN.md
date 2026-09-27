# Getbrick 框架设计

> 状态：**待评审**。定稿后才开始写代码。

## 1. 定位

业务项目用 **DDD** 组织代码。Getbrick 提供两样东西：

1. `getbrick-ddd` — 领域层原语，纯 Java，零框架依赖
2. 一组能力仓库 — MyBatis-Plus 集成、事件、租户、数据权限、缓存、Web。每个单独一个仓库，
   业务方**按需引依赖 + 打 `@EnableXxx` 注解**才生效

**不进 Getbrick 的**：每个项目写法不一样的、需要业务决策的、能被成熟库直接替代的。

工具库统一用 **Apache Commons**。**不使用 Hutool**。

## 2. 技术底座

版本均为 2026-09 从 Maven Central 核实。

| 项 | 选型 | 版本 |
| --- | --- | --- |
| JDK | Oracle JDK | 25 |
| 框架 | Spring Boot | 4.1.1（Spring Framework 7.0.9 / Jakarta EE 11 / Servlet 6.1） |
| 持久层 | MyBatis-Plus | 3.5.17（`mybatis-plus-spring-boot4-starter`） |
| 多数据源 | dynamic-datasource | 4.5.0（`-spring-boot4-starter`） |
| API 文档 | springdoc-openapi | 3.1.1 |
| 缓存 / 锁 | Redisson | 4.7.0 |
| 本地缓存 | Caffeine | 3.3.0（Spring Boot 已管版本） |
| UUID | java-uuid-generator | 5.2.0（UUIDv7 / ZeroUUID） |
| 等值性测试 | EqualsVerifier | 4.5.2 |
| 分层约束测试 | ArchUnit | 1.5.1 |
| 熔断限流 | resilience4j | 2.4.0 |

### 2.1 Apache Commons（不用 Hutool）

Spring Boot 4.1.1 已经管了 `commons-lang3`、`commons-codec`、`commons-pool2`、
`commons-dbcp2`、`commons-logging`，这五个不再重复声明。

由 `getbrick-dependencies` 补齐的：

| 制品 | 版本 | 用途 |
| --- | --- | --- |
| `commons-io` | 2.22.0 | 文件 / 流 |
| `commons-text` | 1.15.0 | 字符串算法、模板替换 |
| `commons-collections4` | 4.6.0 | 集合工具 |
| `commons-math3` | 3.6.1 | 数值 |
| `commons-compress` | 1.28.0 | 压缩归档 |
| `commons-csv` | 1.14.1 | CSV |
| `commons-configuration2` | 2.15.1 | 配置抽象 |
| `commons-jexl3` | 3.7.0 | 表达式求值 |

`commons-cli` 和 `commons-validator` 已从 Central 的 `org/apache/commons` 路径移除，
最后一次发布停留在 1.11.0（2021），不采用。

**约定：Getbrick 内不写通用工具类。** 需要什么从 Commons 取；Commons 没有的，
先想清楚是不是业务代码本身的问题。

### 2.2 Boot 4 的两个坑

| 变化 | 旧 | 新 |
| --- | --- | --- |
| AOP starter | `spring-boot-starter-aop` | **`spring-boot-starter-aspectj`**（旧的已移除） |
| Jackson | `com.fasterxml.jackson:jackson-bom` 2.x | **`tools.jackson:jackson-bom` 3.x**，包名 `tools.jackson.*` |

## 3. 版本管理的归属

**Spring Boot 能管的一律不管。** `getbrick-dependencies` 改为 import
`spring-boot-dependencies:4.1.1`，再补它不覆盖的：MyBatis-Plus、dynamic-datasource、
Redisson、Commons 那 8 个、resilience4j、java-uuid-generator、EqualsVerifier、ArchUnit、
transmittable-thread-local。

由 Spring Boot BOM 接管：Spring Framework、Jackson 3、JUnit 6、Hibernate Validator 9、
Testcontainers、Logback、SLF4J、Micrometer、Caffeine、`commons-lang3` 等五个。

现有 BOM 里锁的 Jackson 2.22.3 和 JUnit 5.14.4 必须换掉，否则和 Boot 4 正面冲突。

## 4. 仓库划分与依赖

```
                          getbrick-build
                          (父 POM + BOM)
                                │
                          getbrick-ddd
                     (纯 Java，无 Spring)
                                │
        ┌───────────┬───────────┼───────────┬──────────────┐
        │           │           │           │              │
  getbrick-     getbrick-   getbrick-   getbrick-    getbrick-
   event        cache       web     mybatis-plus      │
  （可选）        │           │           │              │
                   │     ┌─────┴─────┐        │
                   │     │           │        │
             getbrick-  getbrick-      │
               tenant  datascope      │
                                          │
                                       （roadmap）
```

依赖方向单向，无环。硬规则：

- `getbrick-ddd` **不依赖 Spring、不依赖任何框架**。领域层引容器就是退化成事务脚本。
- 能力仓库之间：`tenant` 和 `datascope` 依赖 `mybatis-plus`，因为它们要挂进
  MyBatis-Plus 的拦截器链；其余能力仓库互不依赖。
- 依赖方向由 `maven-enforcer-plugin` 的 `bannedDependencies` 在 CI 卡住。

| 仓库 | 类型 | 依赖 | 职责 |
| --- | --- | --- | --- |
| `getbrick-ddd` | 库 | 仅 JSpecify | 领域原语 + 分层约束规则 |
| `getbrick-event` | starter（可选） | ddd | 事件与消息：应用事件、事务提交后触发、MQ 投递 SPI |
| `getbrick-cache` | starter | ddd | 聚合级缓存、分布式锁、缓存一致性 |
| `getbrick-web` | starter | ddd | 统一响应、全局异常、traceId、参数校验增强 |
| `getbrick-mybatis-plus` | starter | ddd | Repository / UnitOfWork 实现、审计填充、乐观锁、分页 |
| `getbrick-tenant` | starter | mybatis-plus | 租户上下文、SQL 自动拼租户条件、旁路开关 |
| `getbrick-datascope` | starter | mybatis-plus | `@DataScope` 注解 + SQL 改写 |

roadmap（先不起仓库）：`getbrick-idempotent`、`getbrick-ratelimit`、
`getbrick-openapi`、`getbrick-config`、`getbrick-operation-log`。

## 5. 开启方式：依赖 + `@Enable`

**不用 auto-configuration 默认打开。** 引依赖只是把类放进 classpath，
必须显式打注解才生效。理由：DDD 项目对横切能力的取舍差异很大，
默认全开等于替业务方做决定。

```java
@SpringBootApplication
@EnableMybatisPlusSupport      // Repository / UoW / 审计填充 / 乐观锁
@EnableWebSupport             // 统一响应 / 全局异常 / traceId
public class Application {}    // 就这两个，DDD 就能跑
```

需要更多能力时按需加：

```java
@SpringBootApplication
@EnableMybatisPlusSupport
@EnableWebSupport
@EnableTenant                  // 多租户隔离
@EnableDataScope               // 数据权限
@EnableCacheSupport            // 聚合缓存 + 分布式锁
@EnableDomainEvent             // 事件解耦，可选；不加就走同步写库
public class Application {}
```

注解本身只是 `@Import`：

```java
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TenantAutoConfiguration.class)
public @interface EnableTenant {

    /** 忽略租户条件的表。默认读 getbrick.tenant.ignore-tables */
    String[] ignoreTables() default {};
}
```

被 `@Import` 的配置类同时满足两个条件，使得注解和配置两条路都可用：

```java
@AutoConfiguration
@ConditionalOnClass(TenantLineHandler.class)
@ConditionalOnProperty(prefix = "getbrick.tenant", name = "enabled", havingValue = "true")
@ConditionalOnMissingBean(TenantContext.class)
public class TenantAutoConfiguration {
}
```

- 打 `@EnableTenant` → 直接 `@Import`，一定生效
- 不打注解但设 `getbrick.tenant.enabled=true` → 走 `AutoConfiguration.imports`，也生效
- `@ConditionalOnMissingBean` → 业务方可用自己的 Bean 覆盖框架实现
- `@ConditionalOnClass` → 依赖缺失时静默跳过，不会因为漏引依赖而启动失败

`@EnableXxx` 里的参数只放**代码级**决策（哪个表不隔离、缓存 TTL）。
放 `application.yml` 的是**环境级**决策（是否启用、连接哪个 Redis）。

## 6. getbrick-ddd 设计

零 Spring 依赖。只用 JSpecify 做 null 标注（Spring Boot 4 自己也在用）。

### 6.1 标识

| 类型 | 说明 |
| --- | --- |
| `Identifier` | ID 类型的标记接口（订单号、用户号…） |
| `IdGenerator` | ID 生成接口 |
| `DefaultIdGenerator` | 基于 java-uuid-generator 的 UUIDv7 实现。**不自研雪花算法** |

### 6.2 值对象

| 类型 | 说明 |
| --- | --- |
| `ValueObject` | 标记接口。用 `record` 实现 |
| — | 等值性由 **EqualsVerifier** 在测试里断言，不写反射工具 |

```java
public record Money(BigDecimal amount, Currency currency) implements ValueObject {
    public Money {
        if (amount.signum() < 0) {
            throw new InvariantViolation("amount must not be negative");
        }
    }
}
```

```java
class MoneyTest {
    @Test
    void allFieldsParticipateInEquality() {
        EqualsVerifier.forClass(Money.class).verify();
    }
}
```

### 6.3 实体与聚合

| 类型 | 说明 |
| --- | --- |
| `Entity` | 有标识的标记接口 |
| `AggregateRoot` | 聚合根标记接口。**纯标记，不带任何事件机制** |

`AggregateRoot` 就是一个空接口。聚合根自己写不变式、自己写业务方法，
这是 DDD 的核心，也是不该被框架代劳的部分。

```java
public class Order extends AggregateRoot {

    private OrderId id;
    private OrderStatus status;
    private List<OrderLine> lines;

    /** 业务方法在聚合根上，不在 service 里 */
    public void pay(Money amount) {
        if (status != OrderStatus.CONFIRMED) {
            throw new DomainException("只有已确认的订单可以支付");
        }
        this.paidAmount = amount;
        this.status = OrderStatus.PAID;
    }
}
```

同步路径到此为止：`Repository.save(order)` → `UnitOfWork.commit()` → 落库。
**这是 Getbrick 的默认路径，绝大多数聚合不需要写任何事件代码。**

聚合之间**不互相持有引用**，通过 ID 关联。这条由 ArchUnit 规则强制。

### 6.4 领域事件（可选）

领域事件是 DDD 的一个**可选**能力，不是 DDD 的必要条件。
同步写库就够用的聚合，不要为了"符合 DDD"而硬塞事件。

需要一个信号时——「订单支付完成」要通知库存、营销、通知——才引入。

| 类型 | 说明 |
| --- | --- |
| `DomainEvent` | 领域层接口：`eventId()` + `occurredAt()` |
| `DomainEventBuffer` | 组合式的收集器。聚合**持有它**而不是继承它 |
| `DomainEventPublisher` | 发布接口。实现放 `getbrick-event`，不引也能用 |

用组合而不是继承，是为了让"要不要事件"变成聚合自己的字段决定，
而不是继承树决定：

```java
public class Order extends AggregateRoot {

    /** 不需要发事件的聚合，这个字段就是 null */
    private final DomainEventBuffer events = new DomainEventBuffer();

    public void pay(Money amount) {
        if (status != OrderStatus.CONFIRMED) {
            throw new DomainException("只有已确认的订单可以支付");
        }
        this.paidAmount = amount;
        this.status = OrderStatus.PAID;
        events.record(new OrderPaid(id, amount, clock.instant()));   // 可选
    }

    /** 同步路径：直接存就行 */
    List<DomainEvent> pendingEvents() {
        return events.drain();
    }
}
```

`getbrick-ddd` 里的这部分是 15 行的值对象，没有任何框架依赖。
发布、投递、重试、MQ 对接全在 `getbrick-event`——**不引这个仓库，
领域事件就只是躺在内存里的几个对象，不会有任何副作用。**

**不做**：事件溯源（Event Sourcing）框架、事件存储、事件回放。
这是另一个量级的复杂度，个别项目真要做时在项目内做，不要塞进公共框架。

### 6.5 领域异常

| 类型 | 说明 |
| --- | --- |
| `DomainException` | 业务规则被违反的基类 |
| `InvariantViolation` | 不变式被破坏 |
| `AggregateNotFound` | 聚合不存在 |
| `ConcurrentModification` | 乐观锁版本冲突 |

这些都是 **业务可预期的失败**，不是编程错误。
HTTP 状态码映射在 `getbrick-web` 里做，领域层不感知 HTTP。

### 6.6 仓储与工作单元

| 类型 | 说明 |
| --- | --- |
| `Repository<A, ID>` | 聚合仓储接口：`findById` / `save` / `delete` |
| `UnitOfWork` | `registerNew` / `registerModified` / `remove` / `commit` / `rollback` |
| `UnitOfWorkManager` | 取当前工作单元的接口。ThreadLocal 实现放 `getbrick-mybatis-plus` |

仓储**只针对聚合根**。不是聚合根的对象没有自己的仓储，要通过聚合导航。
这条是 DDD 的硬约束，由 ArchUnit 规则强制。

### 6.7 规约与服务

| 类型 | 说明 |
| --- | --- |
| `Specification<T>` | 可复用业务谓词，支持 `and` / `or` / `not` 组合 |
| `DomainService` | 跨聚合、不属于任何一个聚合的业务操作 |
| `DomainFactory<A>` | 复杂聚合的创建入口 |

### 6.8 分层约束（ArchUnit）

**这是 `getbrick-ddd` 最实际的价值**：把 DDD 的规矩变成能跑的测试，
而不是靠 code review 盯。

以 `test-jar`（classifier `tests`）发布，业务项目直接复用：

```java
@AnalyzeClasses(packages = "com.example.order")
@ArchTest
static final ArchRule 领域层不得依赖框架层 = layeredArchitecture()
        .consideringOnlyDependenciesInLayer()
        .layer("Domain").definedBy("..domain..")
        .layer("Application").definedBy("..application..")
        .layer("Infrastructure").definedBy("..infrastructure..")
        .whereLayer("Domain").mayNotBeAccessedByAnyLayer();

@ArchTest
static final ArchRule 领域层不得依赖Spring = noClasses()
        .that().resideInAPackage("..domain..")
        .should().dependOnClassesThat()
        .resideInAnyPackage("org.springframework..", "com.baomidou..", "jakarta.persistence..");
```

预置规则清单：

1. 领域层不得依赖 Spring / MyBatis / JPA / Web
2. 领域层不得访问应用层、基础设施层
3. 聚合不得直接引用其他聚合类型
4. 值对象必须是 `final` 的 record 或 final 类
5. 仓储接口必须以 `Repository` 结尾
6. 领域异常不得继承 `RuntimeException`（必须走 `DomainException`）

规则本身用 ArchUnit 官方 API 写，不造轮子。

## 7. 各能力仓库职责

### 7.1 getbrick-mybatis-plus

| 内容 | 用什么现成的 |
| --- | --- |
| `Repository` / `UnitOfWork` 的 MP 实现 | MyBatis-Plus `BaseMapper` + 拦截器链 |
| 聚合 ↔ 表的映射约定 | `@TableName` / `@TableId`（`IdType.INPUT`，ID 由聚合自己生成） |
| 审计字段自动填充 | MP `MetaObjectHandler` |
| 乐观锁 | MP `OptimisticLockerInnerInterceptor` |
| 分页 | MP `PaginationInnerInterceptor` + `PageResult<T>` 转换器 |
| 多数据源 | dynamic-datasource |

`PageResult<T>` 定义在这里而不是 `getbrick-ddd`——DDD 的聚合不分页，
分页属于查询侧（读模型），属于基础设施。

**不做**：通用 CRUD 基类。项目间口径差太大，必然变成垃圾场。

### 7.2 getbrick-event（可选）

**不引这个仓库，Getbrick 一样能用。** 只有需要事件解耦、跨服务通知、
最终一致性时才引。`Repository.save()` 同步落库永远可用，两条路并存。

| 内容 | 说明 |
| --- | --- |
| `SpringDomainEventPublisher` | `DomainEventPublisher` 的 Spring 实现。发到 `ApplicationEventPublisher` |
| `SpringEventTransport` | 纯进程内投递。**默认实现，`@EnableDomainEvent` 后即可用** |
| `KafkaTransport` / `RocketMQTransport` | SPI 实现。**不起独立仓库**，放业务项目或需要时再拆 |
| `OutboxTransport` | 业务表做发件箱，保证「本地事务提交」与「事件发出」最终一致 |
| 事务后触发 | 聚合保存后、事务提交后，才真正发出去 |

```java
public interface EventTransport {
    void publish(List<DomainEvent> events);
}
```

`EventTransport` 做成 SPI 是关键：**框架不替业务选 MQ**。
默认实现只发 Spring 事件，跨进程的事由业务侧决定用什么中间件。

`@EnableDomainEvent(transport = EventTransport.SPRING_EVENT)` 里，
不传参就是进程内。

### 7.3 三种实现路径的成本

| 路径 | 什么时候选 | 代价 | Getbrick 支持 |
| --- | --- | --- | --- |
| 同步写库（默认） | 聚合内改完立即要看到结果；强一致 | 无 | 全部仓储接口 + UoW |
| 事件解耦 | 一次业务要通知多方；下游可延迟 | 引入最终一致性，要处理重复、乱序、补偿 | 可选 `@EnableDomainEvent` + `EventTransport` SPI |
| 事件溯源 | 需要完整历史、需要时间旅行重放 | 极高：事件存储、快照、重放、回放测试 | **不支持**，留在项目内自建 |

判据很简单：**下游失败要不要影响主流程**。不要，就同步写库。
要，且能接受短暂不一致，才上事件。别因为"DDD 好像就该有事件"而上。

### 7.3 getbrick-tenant

| 内容 | 说明 |
| --- | --- |
| `TenantContext` | 当前租户，读写走 `ThreadLocal` + 异步场景透传 |
| `TenantLineInnerInterceptor` 装配 | 挂进 MP 拦截器链 |
| `@TenantIgnore` | 方法 / 类 / 表级旁路 |
| 租户切换校验 | 跨租户写操作在 `commit` 时检查 |

异步场景的租户透传用 **transmittable-thread-local**，不自己实现线程池装饰器。

### 7.4 getbrick-datascope

| 内容 | 说明 |
| --- | --- |
| `@DataScope` | 声明数据范围（全部 / 本部门 / 本部门及以下 / 仅本人 / 自定义） |
| `DataScopeInnerInterceptor` | 解析注解，改写 SQL 追加 `WHERE` 条件 |
| `DataScopeProvider` SPI | 从上下文取当前用户部门 / 角色 |

改写 SQL 有两种做法：MP 的 `DataPermissionInterceptor`，或 JSQLParser 自己拼。
先用 MP 的，它够用；不够再换。**不自己写 SQL AST**。

### 7.5 getbrick-cache

| 内容 | 说明 |
| --- | --- |
| `AggregateCache` | 按聚合 ID 缓存聚合本身，key = 聚合类型 + ID |
| 分布式锁 | Redisson 封装，统一锁命名和释放时机 |
| 本地缓存 | Caffeine 一级 + Redisson 二级，两级一致性用「写后失效」 |

**不用 `@Cacheable` 缓存聚合**。聚合是活的领域对象，被缓存后和在内存里改的是
同一个对象，`@Cacheable` 拿不到引用、拿不到脏检查。这里用显式的
`AggregateCache.load(aggregateId, supplier)`。

### 7.6 getbrick-web

| 内容 | 说明 |
| --- | --- |
| `Result<T>` | 统一响应信封，成功与失败同一个形状 |
| `ErrorCode` / `ErrorCodeRegistry` | 错误码。格式 `GETBRICK-<模块><序号>`，如 `GETBRICK-DDD-0401` |
| `GlobalExceptionHandler` | `@RestControllerAdvice`，`DomainException` → `Result` |
| traceId | 生成或透传，写 MDC + `X-Trace-Id` 响应头 |
| 参数校验增强 | 分组校验、错误信息聚合、错误路径提取 |

失败响应用 Spring 的 `ProblemDetail`（RFC 9457）做内容来源，
`Result` 负责把它包成统一信封。这样错误序列化不重造。

**不做**：鉴权。用 Spring Security，不自己写。
**不做**：在异常处理器里塞业务逻辑。处理器只做格式转换。

## 8. 开关约定

```yaml
getbrick:
  mybatis-plus:
    audit:
      enabled: true
    optimistic-lock:
      enabled: true
  event:                        # 不引 getbrick-event 就没有这段
    transport: spring-event     # spring-event | kafka | rocketmq | outbox
  tenant:
    enabled: false
    ignore-tables: [sys_dict, sys_config]
  data-scope:
    enabled: false
  cache:
    enabled: true
    local-ttl: 30s
  web:
    trace-id:
      enabled: true
    result-envelope: true       # 关掉则直接返回原始对象
```

命名空间统一 `getbrick.*`。所有能力都有开关；关掉后不注册任何 Bean。

## 9. 明确不做

| 不做 | 原因 |
| --- | --- |
| Hutool | 统一用 Apache Commons |
| 通用工具类 | Commons 已有，或说明是业务问题 |
| 雪花 ID 算法 | 用 java-uuid-generator 的 UUIDv7 |
| 通用 CRUD 基类 | 项目间口径不一致 |
| 鉴权 / 权限 | Spring Security |
| 限流令牌桶 | resilience4j `RateLimiter` |
| 配置中心客户端 | Nacos / Apollo 各家不同，做成 SPI 由项目实现 |
| MQ 具体实现 | 同上，框架不替业务选 MQ；做成 SPI 由项目实现 |
| 事件溯源框架 / 事件存储 / 事件回放 | 另一个量级的复杂度，留在项目内自建 |
| 强制所有聚合发事件 | DDD 不要求。同步写库是默认路径 |
| 值对象相等性工具 | EqualsVerifier |
| SQL AST 改写 | MyBatis-Plus 拦截器链，不够再换 JSQLParser |
| JPA / Hibernate 集成 | 有 MP 就不再来一套 |
| 领域层引 Spring | DDD 的前提 |

## 10. 风险

| 风险 | 说明 | 应对 |
| --- | --- | --- |
| MyBatis-Plus 3.5.17 传递依赖被报 CVE-2026-41001 | 需确认受影响范围 | 开工前跑漏洞扫描；不接受的话数据层要换方案，仓库划分跟着变 |
| `DomainEventBuffer` 自己写 | 和 spring-data 的 `AbstractAggregateRoot` 功能重叠 | 已论证：领域层不能引 spring-data；且用组合而非继承，不用事件的聚合零成本。15 行，评审重点在这里 |
| Boot 4 生态仍在补齐 | 部分库只出到 Boot 3 | 第 2 节逐个核过版本；依赖只进 BOM，不进业务仓库 |
| 多仓版本漂移 | 7 个仓库各自发版 | 全组织同版本号，BOM 统一对齐，`getbrick-ddd` 版本作为下限 |
| `@Enable` 数量膨胀 | 7 个能力 = 7 个注解，启动类变长 | 接受。真嫌烦可以自己写一个聚合注解，但那属于业务代码 |

## 11. 实施顺序

| 步 | 内容 | 阻塞关系 |
| --- | --- | --- |
| 1 | 改 `getbrick-dependencies`：import `spring-boot-dependencies`，换成 Commons，移除 Hutool | 无 |
| 2 | `getbrick-core` → `getbrick-ddd`（改名），写领域原语 + ArchUnit 规则 | 依赖 1 |
| 3 | `getbrick-mybatis-plus` | 依赖 2。**同步路径的主干**：Repository / UoW 实现，没它 DDD 跑不起来 |
| 4 | `getbrick-web` | 依赖 2、3。异常 → 响应的映射需要 `DomainException` 和仓储异常 |
| 5 | `getbrick-tenant` | 依赖 3。挂 MP 拦截器链 |
| 6 | `getbrick-datascope` | 依赖 3、5 |
| 7 | `getbrick-cache` | 依赖 2。与前面无耦合，可并行 |
| 8 | `getbrick-event` | 依赖 2、3。**可选**：同步路径通了之后再说，不阻塞其他仓库 |

前 4 步走完就是一条完整可用的同步 DDD 链路。`event` 排最后是因为
它是加法，不是前提。

每个仓库：建 pom → 拷 `config/` 和 wrapper → 拷 CI 模板 → 写代码。
`scripts/install-build-config.sh` 一步到位。
