# Proposal

## Why

The package graph is acyclic, but almost every collaboration is concrete to
concrete: `RefundPolicy`, `RefundService`, `RefundTools`, `CardDirectory`, and
the eval harness all depend on the in-memory `DemoData` seed, `domain` carries a
Spring annotation, and `RefundAssistantService` depends directly on Micrometer
`Observation`. Replacing storage, moving the HTTP adapter, or swapping telemetry
therefore requires editing core classes. This change introduces consumer-owned
ports so those seams exist without changing runtime behavior.

## What Changes

- Introduce `AccountRepository`, `ChargeRepository`, and `RefundLedger` ports
  around the demo seed. `RefundLedger` becomes an interface; an in-memory
  implementation preserves today's behavior.
- Move `DemoData` and the in-memory ledger out of `domain` into a new
  `infrastructure` package. `DemoData` becomes an adapter implementing the
  account and charge ports and the `AccountLookup` port.
- Give `CardDirectory` a consumer-owned `AccountLookup` port instead of a
  direct dependency on `DemoData`.
- Replace `RefundAssistantService`'s direct use of `TurnObservation` and
  Micrometer `Observation` with a `TurnTelemetry` port. The Micrometer
  implementation moves to `observability`.
- Move `AssistantController` from `assistant` to a new `web` package.
- Rewire `PolicyConfiguration`, `RefundTools`, `RefundPolicy`, `RefundService`,
  `EvalRunner`, and affected tests to depend on port types.
- No HTTP contract, guardrail order, policy decision, telemetry attribute, eval
  case, or configuration key changes.

## Capabilities

### New Capabilities

None. This is a pure internal refactor with no spec-level behavior change, so
the change sets `skip_specs: true`.

### Modified Capabilities

None.

## Impact

- Code: `domain`, `policy`, `security`, `assistant`, `observability`,
  `infrastructure` (new), `web` (new), `evals`.
- Spring wiring: beans are bound to port types; component scanning still covers
  all packages under `dev.diegap.refundguardrails`.
- Tests: package references and constructor calls are updated; assertions and
  fixtures remain unchanged.
- No public HTTP API, YAML schema, configuration property, or dependency
  changes.
