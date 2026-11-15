# Tasks

## 1. Move the two driven adapters to `store`

- [x] 1.1 `git mv` `DemoData.java` and `InMemoryRefundLedger.java` from
  `src/main/java/dev/diegap/refundguardrails/infrastructure/` to
  `.../refundguardrails/store/` and change their `package` line; verify with
  `./gradlew compileJava` and confirm
  `grep -rn "refundguardrails\.infrastructure" src/main/java` returns nothing.
- [x] 1.2 `git mv` `DemoDataTest.java` and `InMemoryRefundLedgerTest.java` to the
  matching `store` test package and change their `package` line; verify with
  `./gradlew test --tests "dev.diegap.refundguardrails.store.*"`.
- [x] 1.3 Update the imports in `security/CardDirectoryTest`,
  `policy/RefundPolicyTest`, and `policy/RefundServiceTest` to
  `dev.diegap.refundguardrails.store`; verify with `./gradlew test` and confirm
  no assertions changed by reviewing `git diff --stat src/test`.

## 2. Documentation and integration

- [x] 2.1 Update the README package table row (`observability`, `store`) and the
  hexagonal-layout mermaid node to read `store`; verify with
  `grep -rn "infrastructure" README.md` returning nothing and confirm the table
  text matches the actual package under `src/main/java`.
- [x] 2.2 Run `./gradlew test` and `./gradlew eval`; verify the suite and all
  eval cases pass with the same counts as before the rename, and that
  `grep -rn "refundguardrails\.infrastructure" src` returns nothing.

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result.
