# Getbrick DDD

业务项目用 DDD 组织代码，这个仓库提供**领域层原语**和**分层约束的自动化测试**。

构建配置与依赖版本来自
[getbrick-build](https://github.com/getbrick/getbrick-build)，本仓库**零 Spring 依赖**。

## 状态

**设计待评审，代码尚未开始。** 方案见 [docs/DESIGN.md](docs/DESIGN.md)，评审通过后才写代码。

## 边界

这个仓库**只放**领域层要用的东西：

- 标识与 ID 生成
- 值对象
- 实体、聚合根
- 仓储接口、工作单元
- 规约、领域服务、领域工厂
- 领域异常
- 领域事件接口（可选能力，发布机制在 `getbrick-event`）
- ArchUnit 分层约束规则（`test-jar` 形式，业务项目直接复用）

**不放**这些，它们各自有独立仓库：

| 能力 | 仓库 | 开启方式 |
| --- | --- | --- |
| MyBatis-Plus 集成、审计填充、乐观锁、分页 | `getbrick-mybatis-plus` | `@EnableMybatisPlusSupport` |
| 统一响应、全局异常、traceId、参数校验 | `getbrick-web` | `@EnableWebSupport` |
| 多租户隔离 | `getbrick-tenant` | `@EnableTenant` |
| 数据权限 | `getbrick-datascope` | `@EnableDataScope` |
| 聚合缓存、分布式锁 | `getbrick-cache` | `@EnableCacheSupport` |
| 事件与消息（可选） | `getbrick-event` | `@EnableDomainEvent` |

同步写库是默认路径，不需要事件、不需要引 `getbrick-event`。

## 开发

```bash
# 首次：父 POM 和 BOM 装进本地仓库
git clone https://github.com/getbrick/getbrick-build.git
(cd getbrick-build && ./mvnw -DskipTests install)

./mvnw verify              # 编译 + 单测 + Checkstyle + Spotless + Enforcer
./mvnw -Pquality verify     # 追加 SpotBugs、覆盖率下限、依赖使用分析
./mvnw spotless:apply       # 格式化并补 license header
```

改动 `config/` 前先同步上游，避免与其他仓库产生分叉：

```bash
../getbrick-build/scripts/install-build-config.sh .
```

CI 的 `config-drift` job 会校验 `config/` 与 getbrick-build 一致。

## 发版

```bash
git tag v1.0.0 && git push origin v1.0.0
```

tag 触发的 workflow 用同名的 `getbrick-build` tag 作为父 POM，一起签名发到
Maven Central。密钥与 Central Portal token 存在 `maven-central` environment。

## License

[Apache-2.0](LICENSE)
