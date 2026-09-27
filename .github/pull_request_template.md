## What changed

<!-- One or two sentences. Link the issue with "Closes #123". -->

## Type of change

- [ ] Dependency or plugin version bump
- [ ] Build pipeline / CI
- [ ] Checkstyle or Spotless configuration
- [ ] New profile or Maven property
- [ ] Documentation

## Why

<!-- The problem this solves. Skip for mechanical version bumps. -->

## How it was verified

- [ ] `./mvnw verify` passes locally
- [ ] `./mvnw -Pquality verify` passes locally
- [ ] `./mvnw -Pstrict validate` passes locally
- [ ] Reproducible build check passes (two builds produce identical checksums)

## Impact checklist

- [ ] Version bumps are reflected in `getbrick-dependencies/pom.xml` properties
- [ ] Any module consuming this build has been smoke-tested
- [ ] Documentation under `docs/` is updated if behaviour changed
- [ ] No credential, token or key is committed
