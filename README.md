# Getbrick Core

Getbrick 基础框架模块。构建配置与依赖版本统一来自
[getbrick-build](https://github.com/getbrick/getbrick-build)。

## 状态

**设计待评审，代码尚未开始。** 方案见 [docs/DESIGN.md](docs/DESIGN.md)，评审通过后才建模块。

先看的只有构建骨架：wrapper、Spotless / Checkstyle / Enforcer / JaCoCo、
CI 与发版。构建配置来自 [getbrick-build](https://github.com/getbrick/getbrick-build)，
`config/` 由 `scripts/install-build-config.sh` 同步，CI 会校验是否漂移。

## 开发

```bash
git clone https://github.com/getbrick/getbrick-build.git
(cd getbrick-build && ./mvnw -DskipTests install)

./mvnw verify
./mvnw spotless:apply
```

## License

[Apache-2.0](LICENSE)
