# Proposal

## Why

The previous change introduced data and telemetry ports, but the `assistant`
package still mixes the application use case with Spring AI adapters:
`RefundAssistantService` imports `ChatClient`, the advisors and tools share the
package, the driving side depends on a concrete service, and the AI framework
reaches into the core through `TurnContext`. The core/adapter boundary is
therefore invisible, and the application cannot be read or tested without
knowing Spring AI. B-lite closes that gap without reimplementing the tool loop
or the advisor chain.

## What Changes

- Add the driving port `assistant.port.in.RefundAssistant`; `web`, `demo`, and
  the eval harness depend on it instead of `RefundAssistantService`.
- Add the driven port `assistant.port.out.AssistantModel` with a small
  `ModelReply`/`ModelFailure` contract. The service asks for a reply; the
  adapter owns `ChatClient`, the advisor chain, and the tool loop.
- Move Spring AI-specific classes to a new `ai` package: `AssistantConfiguration`,
  the four advisors, `RefundTools`, `AssistantResponses`, and `AdvisorOrder`.
  Add an adapter-local `AdvisorContext` helper for extracting `TurnContext`.
- Move `TurnTelemetry` to `assistant.port.out` and keep
  `MicrometerTurnTelemetry` as its driven adapter.
- Remove all `org.springframework.ai` imports from the `assistant` package.
  `RefundAssistantService` depends only on `AssistantModel`, `OutputRedactor`,
  and `TurnTelemetry`.
- Expose the safe-failure replies through the application API so eval assertions
  do not reach into the concrete service.
- Leave `domain`, `policy`, `security`, `infrastructure`, the in-memory stores,
  and the vault unchanged. The tool-calling loop stays in the adapter.
- No HTTP contract, guardrail order, policy decision, telemetry attribute, eval
  case, or configuration key changes.

## Capabilities

### New Capabilities

None. This is a structural refactor with no spec-level behavior change, so the
change sets `skip_specs: true`.

### Modified Capabilities

None.

## Impact

- Code: `assistant` (application core), new `ai` (Spring AI adapter), `web`,
  `demo`, `evals`, and affected tests.
- Spring wiring: `RefundAssistant` and `AssistantModel` beans replace direct
  `RefundAssistantService`/`ChatClient` dependencies at the edges; component
  scanning still covers all packages under `dev.diegap.refundguardrails`.
- Tests: package and type references change, but assertions and fixtures remain
  the same.
- No public HTTP API, YAML schema, configuration property, or dependency
  changes.
