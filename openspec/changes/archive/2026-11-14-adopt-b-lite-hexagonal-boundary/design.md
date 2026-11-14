# Design

## Context

See `proposal.md` — Why. The starting point is the tree after the previous
refactor: `domain` is framework-free, `infrastructure` holds the data adapters,
`observability` holds the telemetry adapter, and `web` is the HTTP adapter. The
remaining mixed package is `assistant`:

- `RefundAssistantService` imports `ChatClient`, `ChatClientResponse`, and
  `ToolExecutionException`; it also owns the model input markers and failure
  classification.
- `TurnContext` imports `ChatClientRequest` for `require(...)`.
- `AssistantConfiguration`, `AssistantResponses`, `AdvisorOrder`, the four
  advisors, and `RefundTools` are Spring AI classes in the same package as the
  use case.
- `web`, `demo`, and `eval` depend on the concrete `RefundAssistantService`;
  `EvalAssertions` also reads its safe-reply constants.

There are 130 tests and 15 eval cases that define the current behavior. The
change must preserve both without changing test assertions.

## Goals / Non-Goals

**Goals:**

- Make `assistant` the application core with no `org.springframework.ai`
  imports; define an explicit driving port and the outbound ports it uses.
- Move Spring AI adapters into one adjacent `ai` package.
- Keep the `ChatClient` tool loop, advisor ordering, and tool registration
  intact; do not rebuild them.
- Keep the application readable: `chat` remains the only entry point, and the
  adapter boundary is one small interface.

**Non-Goals:**

- Renaming `assistant` to `application`; the package name stays, and the design
  records that it is the application layer.
- A general model abstraction (messages, tools, generations). The port returns
  final text and a coarse failure kind only.
- Porting `SessionAccounts`, `EscalationQueue`, `PanVault`, or
  `AuthorizationTokens`.
- Moving `policy`, `security`, or their Spring configuration.
- Changing guardrail behavior, prompt text, telemetry attributes, or eval data.

## Decisions

### 1. Keep `assistant` as the application package; add `ai` for Spring AI

`assistant` becomes the core with `port/in` and `port/out` subpackages. The new
`ai` package is the adapter that owns `ChatClient`, the advisors, the tools, and
the model configuration.

Rationale: renaming 26 files and their tests to an `application` package adds
churn without changing the dependency graph. The package name already describes
the use case; the `port` subpackages and the adjacent `ai` package make the
sides visible.

Alternative considered: a full `application`/`adapter` rename. Rejected for
this change as cosmetic churn; it can be done later if the package names become
the dominant source of confusion.

### 2. Explicit driving port: `assistant.port.in.RefundAssistant`

```java
public interface RefundAssistant {
    AssistantTurn chat(AssistantTurnRequest request);
}
```

`RefundAssistantService implements RefundAssistant`. `web.AssistantController`,
`demo.DemoController`, and `evals.EvalRunner` inject `RefundAssistant`.

Rationale: the driving side becomes explicit, and adapters no longer depend on
the concrete service. The interface is one method and costs almost nothing.

Alternative considered: leave the concrete service as the inbound API. That
keeps the dependency direction correct, but hides the driving side and forces
`EvalAssertions` to reach into the service for constants.

### 3. Thin driven port: `assistant.port.out.AssistantModel`

```java
public interface AssistantModel {
    ModelReply converse(TurnContext turn, String modelInput);
}

public record ModelReply(String text) {
}

public final class ModelFailure extends RuntimeException {
    public enum Kind { TOOL, PROVIDER }
    // kind + cause
}
```

`ai.SpringAiAssistantModel` implements it. It owns the prompt construction
(`user(modelInput)`, advisors parameter, tool context), the `ChatClient` call,
response text extraction, and the tool/provider exception classification. The
service catches `ModelFailure`, maps `Kind.TOOL` to `FailureCategory.TOOL_ERROR`
and everything else to `PROVIDER_ERROR`, and keeps the existing safe replies.

`RefundAssistantService.chat` still prefers `turn.finalReply()` when an input
guard short-circuited the chain; otherwise it scans `reply.text()` through the
output redactor. This preserves the current behavior exactly.

Rationale: this inverts the framework dependency without inventing a second
message/tool model. The adapter keeps Spring AI’s tool loop and advisor chain;
the core only sees “give me the final text or a typed failure”.

Alternative considered: no gateway, keeping `ChatClient` in the service. That
is a package move only and leaves Spring AI in the core.
Alternative considered: a full `ModelGateway` with message, tool, and response
types. Rejected because it duplicates Spring AI and makes the happy path harder
to follow.

### 4. Move framework classes to `ai`

Move to `dev.diegap.refundguardrails.ai`:

- `AssistantConfiguration`
- `AdvisorOrder`
- `AssistantResponses`
- `CardRedactionAdvisor`
- `PromptInjectionGuardAdvisor`
- `TopicScopeGuardAdvisor`
- `OutputScanningAdvisor`
- `RefundTools`
- `SpringAiAssistantModel`
- `AdvisorContext` (new, package-private)

`AdvisorContext.require(ChatClientRequest)` replaces `TurnContext.require`; this
removes the last Spring AI import from `assistant`. The advisors use it.

`RefundTools` is an inbound adapter for model tool calls, but it is registered
on the outbound `ChatClient`. Keeping it in `ai` makes the bidirectional AI
boundary visible in one place. `AccountView`, `ChargeView`, and
`RefundToolResult` move with it because they are tool-response shapes.
`EscalationResult` stays in `assistant` because `EscalationQueue` (application
state) produces it.

`InjectionHeuristic` and `TopicScope` stay in `assistant`: they are pure
application logic, not Spring AI.

### 5. Move `TurnTelemetry` into `port.out`; keep its adapter

`assistant.port.out.TurnTelemetry` replaces `assistant.TurnTelemetry`.
`MicrometerTurnTelemetry` imports the port from its new location and otherwise
does not change. This completes the driven-port side and matches the existing
telemetry adapter boundary.

### 6. Safe replies become application API, not service internals

Add `assistant.AssistantReplies` with `SAFE_TOOL_FAILURE` and
`SAFE_PROVIDER_FAILURE`. `RefundAssistantService`, `EvalAssertions`, and the
integration tests use it. The driving port no longer leaks a concrete service
just to read constants.

### 7. Wiring

- `RefundAssistantService` is `@Service` and implements `RefundAssistant`.
- `SpringAiAssistantModel` is `@Component`; it injects the `ChatClient` created
  by `AssistantConfiguration`.
- `PolicyConfiguration` and the rest of the beans are unchanged.
- Component scanning still covers `ai` because it is under the application root.

## Risks / Trade-offs

- **`TurnContext` is mutable and shared across the advisor chain, tools, and the
  service.** → Keep it as the per-turn carrier; move only the framework-specific
  `require` helper. The adapter and service use the same instance passed in the
  port call.
- **Exception classification could drift.** → Move the existing
  `causedByToolFailure` logic verbatim into the adapter and map it to
  `ModelFailure.Kind.TOOL`; keep the service’s log messages and safe replies.
- **Short-circuit behavior could regress.** → The adapter returns the text from
  the advisor response, but the service still checks `turn.finalReply()` first,
  exactly as today.
- **More indirection for one model implementation.** → The port is one method;
  the adapter is the only place that knows Spring AI. This is the accepted cost
  of a framework-free core.
- **Test packages and imports change.** → Update imports and injection types;
  do not change assertions. The integration test may continue to exercise the
  concrete tools adapter because it tests tool behavior, but it should inject
  `RefundAssistant` for turns.
- **No spec covers this behavior.** → The 130 tests and 15 eval cases are the
  acceptance criteria; run both before finishing.

## Migration Plan

1. Add `assistant.port.out.AssistantModel`, `ModelReply`, `ModelFailure`, and
   `AssistantReplies`; make `RefundAssistantService` implement `RefundAssistant`
   and depend on `AssistantModel` instead of `ChatClient`.
2. Add `ai.SpringAiAssistantModel` with the moved call/exception logic, then
   move the AI configuration, advisors, tools, and response helpers to `ai`.
   Remove `TurnContext.require` and add `AdvisorContext`.
3. Move `TurnTelemetry` to `port.out`; update the Micrometer adapter.
4. Rewire `web`, `demo`, `evals`, and tests to the ports and new package.
5. Run `./gradlew test --offline` and `./gradlew eval --offline`; confirm 130
   tests and 15/15 cases. Rollback is a revert of the refactor commit; there is
   no data or deployment migration.

## Open Questions

None.
