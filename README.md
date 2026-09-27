# Getbrick Core

Getbrick 基础框架模块。构建配置与依赖版本统一来自
[getbrick-build](https://github.com/getbrick/getbrick-build)。

## 模块分层

依赖只允许从上往下，反向依赖由 review 把关：

```
getbrick-web → getbrick-expression → getbrick-context → getbrick-beans → getbrick-core → getbrick-common
```

| 模块 | 职责 |
| --- | --- |
| `getbrick-common` | 异常与共享类型。只依赖 JDK |
| `getbrick-core` | 核心原语：生命周期、类型转换、`Ordered` |
| `getbrick-beans` | Bean 元数据、属性绑定、工厂 |
| `getbrick-context` | 应用上下文、环境配置、事件 |
| `getbrick-expression` | 表达式解析与求值 |
| `getbrick-web` | 与传输协议无关的 Web 抽象 |

每个模块目前只有 `package-info.java`——先把边界钉死，再往里填实现。
新加能力前先确认它属于哪一层，不要就近放。

## 开发

```bash
# 首次：先把父 POM 和 BOM 装进本地仓库
git clone https://github.com/getbrick/getbrick-build.git
(cd getbrick-build && ./mvnw -DskipTests install)

./mvnw verify              # 编译 + 单测 + Checkstyle + Spotless + Enforcer
./mvnw -Pquality verify     # 追加 SpotBugs、覆盖率下限、依赖使用分析
./mvnw spotless:apply       # 格式化并补 license header
```

改动 `config/` 之前先同步上游，避免与其他仓库产生分叉：

```bash
../getbrick-build/scripts/install-build-config.sh .
```

CI 里的 `config-drift` job 会校验 `config/` 与 getbrick-build 一致。

## 新增模块

1. 在根 `pom.xml` 的 `<modules>` 里加一行
2. 建目录和 `pom.xml`，`<parent>` 指向 `getbrick-core-parent`
3. 依赖只写 `<groupId>` / `<artifactId>`，**不写 `<version>`**
4. 建 `package-info.java`，写清楚这个模块的职责和它允许依赖谁
5. 设 `jacoco.min.line.coverage`，别让覆盖率下限空着
6. CI 的 `install-build-config.sh` 会自动发现新模块

## 发版

```bash
git tag v1.0.0 && git push origin v1.0.0
```

tag 触发的 workflow 会用同名的 `getbrick-build` tag 作为父 POM，一起签名发到
Maven Central。签名密钥与 Central Portal token 存在 GitHub Secrets 的
`maven-central` environment 里。

## License

[Apache-2.0](LICENSE)
