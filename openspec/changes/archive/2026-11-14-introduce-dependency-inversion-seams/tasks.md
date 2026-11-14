# Tasks

## 1. Domain ports and in-memory adapters

- [x] 1.1 Add `domain.AccountRepository`, `domain.ChargeRepository`, and `domain.AccountLookup`, convert `domain.RefundLedger` to an interface with the current public methods, and add `infrastructure.InMemoryRefundLedger` implementing it; verify with `./gradlew compileJava`.
- [x] 1.2 Make `domain.DemoData` implement `AccountRepository`, `ChargeRepository`, and `AccountLookup` and delegate its ledger accessor to an `InMemoryRefundLedger`, keeping all existing consumers compiling; verify with `./gradlew compileJava`.
- [x] 1.3 Move the ledger assertions out of `DemoDataTest` into a new `infrastructure.InMemoryRefundLedgerTest` and keep `DemoDataTest` limited to seed contents; verify with `./gradlew test --tests "dev.diegap.refundguardrails.domain.DemoDataTest" --tests "dev.diegap.refundguardrails.infrastructure.InMemoryRefundLedgerTest"`.

## 2. Rewire consumers to ports

- [x] 2.1 Change `RefundPolicy` to take `ChargeRepository` and `RefundLedger`, and `RefundService` to take `ChargeRepository` and `RefundLedger`; update `PolicyConfiguration` bean signatures; verify with `./gradlew test --tests "dev.diegap.refundguardrails.policy.*"`.
- [x] 2.2 Change `CardDirectory` to take `domain.AccountLookup` instead of `DemoData`; verify with `./gradlew test --tests "dev.diegap.refundguardrails.security.CardDirectoryTest"`.
- [x] 2.3 Change `RefundTools` to take `AccountRepository`, `ChargeRepository`, and `RefundLedger` instead of `DemoData`, and replace the remaining `data.ledger()` calls; verify with `./gradlew test --tests "dev.diegap.refundguardrails.assistant.RefundAssistantIntegrationTest"`.
- [x] 2.4 Change `EvalRunner` and the Spring tests that only clear or assert ledger state (`DemoPageTest`, `TelemetryObservationTest`, `TelemetryContentEnabledTest`) to inject `RefundLedger` instead of `DemoData`; verify with `./gradlew test --tests "dev.diegap.refundguardrails.evals.*" --tests "dev.diegap.refundguardrails.demo.*" --tests "dev.diegap.refundguardrails.observability.*"`.

## 3. Move the demo adapters out of the domain package

- [x] 3.1 Move `DemoData` and `InMemoryRefundLedger` to `dev.diegap.refundguardrails.infrastructure`, remove `DemoData.ledger()`, and update all main-source imports; verify with `./gradlew compileJava`.
- [x] 3.2 Move `DemoDataTest` to the infrastructure test package and construct the ledger explicitly where needed; verify with `./gradlew test --tests "dev.diegap.refundguardrails.infrastructure.*"`.
- [x] 3.3 Update the remaining test imports and direct constructions, then confirm `domain` has no Spring imports with `grep -rn "org.springframework" src/main/java/dev/diegap/refundguardrails/domain` returning nothing; verify with `./gradlew test`.

## 4. Turn telemetry port

- [x] 4.1 Add `assistant.TurnTelemetry` with a nested `TurnScope` exposing `annotate(TurnMetadata)` and `close()`; verify with `./gradlew compileJava`.
- [x] 4.2 Replace `assistant.TurnObservation` with `observability.MicrometerTurnTelemetry` implementing the port, moving the `refund.turn` span name and every key/value verbatim; verify with `./gradlew test --tests "dev.diegap.refundguardrails.observability.TelemetryObservationTest"`.
- [x] 4.3 Change `RefundAssistantService` to depend on `TurnTelemetry` and manage `TurnScope` in a `try/finally` so metadata is annotated before the span closes; verify with `./gradlew test --tests "dev.diegap.refundguardrails.observability.*" --tests "dev.diegap.refundguardrails.assistant.RefundAssistantIntegrationTest"`.

## 5. Move the HTTP adapter

- [x] 5.1 Move `AssistantController` to `dev.diegap.refundguardrails.web` without changing its annotations, visibility, route, or request/response types; verify with `./gradlew test --tests "dev.diegap.refundguardrails.assistant.RefundAssistantIntegrationTest" --tests "dev.diegap.refundguardrails.demo.DemoPageTest"`.
- [x] 5.2 Confirm no `assistant` class imports `web` or `infrastructure` with `grep -rn "refundguardrails\.\(web\|infrastructure\)" src/main/java/dev/diegap/refundguardrails/assistant` returning nothing; verify with `./gradlew compileJava`.

## 6. Full verification

- [x] 6.1 Run `./gradlew test --offline` and confirm the suite passes with the same test count as before the refactor.
- [x] 6.2 Run `./gradlew eval --offline` and confirm all 15 evaluation cases pass.
- [x] 6.3 Confirm the final dependency direction with `grep -rn "refundguardrails\.infrastructure" src/main/java/dev/diegap/refundguardrails/{domain,policy,security,assistant}` and `grep -rn "io.micrometer" src/main/java/dev/diegap/refundguardrails/assistant`, both returning nothing.

## Workflow follow-up

- Archive the change after review when the user requests it.
