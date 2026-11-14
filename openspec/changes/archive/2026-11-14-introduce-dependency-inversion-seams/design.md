# Design

## Context

See `proposal.md` — Why. The current state constrains the refactor in three
ways:

- `DemoData` is a `@Component` in `domain` that is simultaneously the account
  store, charge store, and ledger holder (`ledger()`); `RefundPolicy`,
  `RefundService`, `RefundTools`, `CardDirectory`, `EvalRunner`, and several
  Spring tests read it directly.
- `RefundAssistantService` drives `TurnObservation`, which returns a Micrometer
  `Observation`; the service therefore imports and manages `Observation` and
  `Observation.Scope` even though the span implementation lives in
  `observability`.
- `AssistantController` is package-private and colocated with the use case; no
  test references the class directly, and the endpoint contract is
  `POST /api/turns`.

The project is a single Spring Boot application, so all new packages are
covered by component scanning and by the existing test classpath.

## Goals / Non-Goals

**Goals:**

- Make policy, tool, security, and assistant code depend on interfaces for data
  access and telemetry.
- Keep `domain` framework-free.
- Make the infrastructure implementations replaceable without editing the
  consumers.
- Keep the package graph acyclic and preserve every existing test assertion,
  HTTP response, span name, span attribute, and eval case.

**Non-Goals:**

- Adding a database, persistence, or any new runtime dependency.
- Splitting `DemoData` into a real repository abstraction with transactions or
  queries.
- Moving `DemoController`/`DemoScenario`, `AssistantConfiguration`
  (`ChatClient` wiring), or the advisor implementations.
- Changing guardrail ordering, policy rules, token semantics, telemetry
  configuration, or the eval YAML schema.
- Introducing a generic `ports`/`adapters` top-level package; ports stay next to
  the consumers or in `domain` where they speak domain types.

## Decisions

### 1. Ports live with the consumer, except repository ports which live in `domain`

- `domain.AccountRepository`, `domain.ChargeRepository`, and
  `domain.RefundLedger` are interfaces expressed in domain terms (`Account`,
  `Charge`, `Refund`).
- `domain.AccountLookup` is a narrow consumer-shaped port:
  `Optional<Account> byPan(String panDigits)`. `CardDirectory` projects the
  result to `CardSummary`, so the seed shape stays out of the security package.
- `assistant.TurnTelemetry` is the port `RefundAssistantService` needs. Its
  nested `TurnScope` exposes `annotate(TurnMetadata)` and `close()`.
- `policy`, `security`, and `assistant` import only these interfaces (plus
  value types), never `infrastructure`.

Rationale: repository interfaces are shared by more than one consumer and use
only domain types, so `domain` is their natural home. `TurnTelemetry` is used by
one consumer and references assistant-owned metadata, so it stays in
`assistant` and the implementation is injected from outside.

Alternative considered: a top-level `ports` package. Rejected because it
separates an interface from the types it is coupled to and adds a package
without clarifying ownership.

Alternative considered: putting `AccountLookup` in `security`. Rejected because
`infrastructure.DemoData` would then depend on `security`, an adapter-to-adapter
edge that is avoidable by placing the port in `domain`.

### 2. `DemoData` becomes an infrastructure adapter; the ledger gets its own adapter

- New package `dev.diegap.refundguardrails.infrastructure`.
- `infrastructure.DemoData` is `@Component` and implements `AccountRepository`,
  `ChargeRepository`, and `AccountLookup`. It keeps the existing fixed seed,
  `accounts()`, `charges()`, and `chargesFor(...)` helpers for the moved
  `DemoDataTest`, but loses the `ledger()` accessor and the `@Component` in
  `domain`.
- `infrastructure.InMemoryRefundLedger` is `@Component` and owns the
  `CopyOnWriteArrayList` previously inside `domain.RefundLedger`.
- `domain.RefundLedger` becomes an interface mirroring the current public
  methods, including `clear()` for eval/test isolation.

Rationale: one adapter can implement several narrow ports without creating
classes that exist only for DI. Keeping `DemoData` as a single seeded adapter
minimizes churn compared with splitting accounts and charges into separate
classes, and a separate ledger adapter removes the `data.ledger()` shortcut.

Alternative considered: `DemoData` implementing `RefundLedger` too. Rejected
because it re-creates the “one object is the whole data layer” coupling this
change is removing and makes the ledger harder to replace.

### 3. Consumers take ports in constructors; `PolicyConfiguration` keeps wiring

- `RefundPolicy(ChargeRepository, RefundLedger, RefundProperties)`.
- `RefundService(RefundPolicy, AuthorizationTokens, ChargeRepository,
  RefundLedger, Clock)`.
- `RefundTools(CardDirectory, SessionAccounts, AccountRepository,
  ChargeRepository, RefundLedger, RefundService, EscalationQueue)`.
- `CardDirectory(PanVault, AccountLookup)`.
- `RefundAssistantService` takes `TurnTelemetry`; its `chat` method calls
  `start(request)`, then in a `try/finally` calls `annotate(turn.metadata())`
  and `close()`. The existing behavior — annotate after the turn even when it
  fails, then stop the span — is preserved.
- `PolicyConfiguration` declares the same beans it does today, with port-typed
  parameters. Spring resolves the single adapter bean for each port.

Rationale: constructor injection keeps the classes framework-free and testable
with plain fakes/stubs; the configuration class remains the composition root for
policy.

### 4. The Micrometer implementation moves to `observability`

- `observability.MicrometerTurnTelemetry` implements `assistant.TurnTelemetry`
  and wraps `ObservationRegistry`. It replaces `assistant.TurnObservation`.
- The span name (`refund.turn`) and every key/value currently set in
  `TurnObservation` move verbatim, so traces are unchanged.
- `observability` now depends on `assistant` for `AssistantTurnRequest`,
  `TurnMetadata`, and `GuardEvaluation`. This is the correct direction for an
  adapter (outer depends on the port), and it creates no cycle because
  `assistant` does not import `observability`.

Alternative considered: leaving `TurnObservation` in `assistant` and having it
implement `TurnTelemetry`. Rejected because the service would still be wired to
a Micrometer-specific class and the “swap telemetry” seam would not exist.

### 5. `AssistantController` moves to a new `web` package unchanged

- New package `dev.diegap.refundguardrails.web`; the class keeps its
  package-private visibility, `@RestController`, `@RequestMapping("/api")`, and
  `POST /turns` method signature.
- `DemoController` stays in `demo`; only the JSON API adapter moves, which is
  the piece that is a pure transport concern.

Rationale: removes the web framework concern from the use-case package without
broadening the change into a UI refactor.

## Risks / Trade-offs

- **A `clear()` method on the `RefundLedger` port leaks a test concern.** →
  Accept it for now to keep eval isolation unchanged; document that production
  code must not call it. A later `Resettable` split is out of scope.
- **`DemoData` implementing three ports could hide accidental coupling.** →
  Each consumer declares only the port it needs; the adapter is the only class
  that sees all three. A future split is mechanical.
- **Spring wiring errors (missing or ambiguous bean) surface at startup.** →
  The existing `@SpringBootTest` suite covers context loading; run it before the
  eval suite.
- **Moving the telemetry adapter adds `observability → assistant`.** → It is an
  acyclic dependency in the adapter direction, and `assistant` remains unaware
  of Micrometer.
- **Tests construct fixtures directly (`new DemoData()`).** → Update constructors
  explicitly; do not add a no-arg production factory just to avoid test edits.
- **Behavior drift in span attributes or ledger sharing.** → Move code verbatim
  and assert through the existing telemetry, policy, integration, and eval
  suites; no new behavior is specified, so the existing assertions are the
  acceptance test.

## Migration Plan

1. Add `domain` ports and move `RefundLedger` to an interface; add the
   infrastructure adapters in the same change so the tree always compiles.
2. Update policy, security, assistant, and eval constructors plus Spring wiring.
3. Move `AssistantController` and `TurnObservation`; update tests and imports.
4. Run `./gradlew test` and `./gradlew eval --offline`; the change is complete
   when both pass with unchanged counts. No data or deployment migration is
   needed; rollback is a revert of the refactor commit.
