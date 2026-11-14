# Tasks

## 1. Application ports and shared replies

- [x] 1.1 Add `assistant.port.in.RefundAssistant`, `assistant.port.out.AssistantModel`, `assistant.port.out.ModelReply`, `assistant.port.out.ModelFailure`, and `assistant.AssistantReplies`; make `RefundAssistantService` implement `RefundAssistant` and use the reply constants; verify with `./gradlew compileJava`.
- [x] 1.2 Point `EvalAssertions` and `RefundAssistantIntegrationTest` at `AssistantReplies` instead of reaching into `RefundAssistantService`; verify with `./gradlew test --tests "dev.diegap.refundguardrails.evals.*" --tests "dev.diegap.refundguardrails.assistant.RefundAssistantIntegrationTest"`.

## 2. Thin model adapter

- [x] 2.1 Add `ai.SpringAiAssistantModel` implementing `AssistantModel`, moving the prompt construction, advisor/tool-context call, response text extraction, and tool/provider exception classification; verify with `./gradlew compileJava`.
- [x] 2.2 Change `RefundAssistantService` to depend on `AssistantModel`, handle `ModelReply` and `ModelFailure`, and remove its `org.springframework.ai` imports while preserving the `turn.finalReply()` short-circuit and output scan; verify with `./gradlew test --tests "dev.diegap.refundguardrails.assistant.RefundAssistantIntegrationTest"`.

## 3. Move Spring AI adapters to `ai`

- [x] 3.1 Add package-private `ai.AdvisorContext` with the `require(ChatClientRequest)` helper, remove `TurnContext.require`, move `AssistantConfiguration`, `AdvisorOrder`, `AssistantResponses`, the four advisors, and `RefundTools` to `ai`, and update all imports; verify with `./gradlew compileJava`.
- [x] 3.2 Move `AccountView`, `ChargeView`, and `RefundToolResult` to `ai`, update `RefundTools` and the integration test, and keep `EscalationResult` in `assistant` because `EscalationQueue` produces it; verify with `./gradlew test --tests "dev.diegap.refundguardrails.assistant.RefundAssistantIntegrationTest"`.
- [x] 3.3 Move `AdvisorOrderTest` to the `ai` test package (or update its imports) so it follows the advisors it verifies; verify with `./gradlew test --tests "dev.diegap.refundguardrails.ai.*"`.
- [x] 3.4 Confirm `assistant` has no Spring AI imports with `grep -rn "org.springframework.ai" src/main/java/dev/diegap/refundguardrails/assistant` returning nothing; verify with `./gradlew compileJava`.

## 4. Telemetry port location

- [x] 4.1 Move `TurnTelemetry` to `assistant.port.out`, update `RefundAssistantService` and `observability.MicrometerTurnTelemetry`, and delete the old file; verify with `./gradlew test --tests "dev.diegap.refundguardrails.observability.*"`.

## 5. Rewire driving adapters

- [x] 5.1 Change `web.AssistantController`, `demo.DemoController`, and `evals.EvalRunner` to depend on `RefundAssistant`, and update tests that only exercise turns to inject the port; verify with `./gradlew test --tests "dev.diegap.refundguardrails.demo.*" --tests "dev.diegap.refundguardrails.evals.*" --tests "dev.diegap.refundguardrails.assistant.RefundAssistantIntegrationTest"`.
- [x] 5.2 Confirm no `web`, `demo`, or `evals` class imports `RefundAssistantService` with `grep -rn "RefundAssistantService" src/main/java/dev/diegap/refundguardrails/{web,demo,evals}` returning nothing; verify with `./gradlew compileJava`.

## 6. Full verification

- [x] 6.1 Run `./gradlew test --offline` and confirm 130 tests pass with 0 failures.
- [x] 6.2 Run `./gradlew eval --offline` and confirm all 15 evaluation cases pass.
- [x] 6.3 Confirm the dependency direction with `grep -rn "org.springframework.ai" src/main/java/dev/diegap/refundguardrails/assistant` and `grep -rn "refundguardrails.ai" src/main/java/dev/diegap/refundguardrails/assistant`, both returning nothing.

## Workflow follow-up

- Archive the change after review when the user requests it.
