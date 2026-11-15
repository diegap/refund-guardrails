# Design

## Context

See `proposal.md` - Why. The tree is organized feature-first; every package
names a capability or a port side, and adapters live next to the capability
they serve (`ai`, `observability`, `scripted`, `web`, `demo`, `evals`).

`infrastructure` is the exception: a layer name holding two in-memory driven
adapters, `DemoData` (implements `AccountRepository`, `ChargeRepository`,
`AccountLookup`) and `InMemoryRefundLedger` (implements `RefundLedger`). No
production class imports the package; it is wired by component scan and
constructed directly by five tests.

Six classes hold in-process state, and only two of them are in this package:
`security.PanVault`, `assistant.EscalationQueue`, `assistant.SessionAccounts`,
and `policy.AuthorizationTokens` hold state too. The package therefore cannot
mean "everything in memory".

## Goals / Non-Goals

**Goals:**

- Make the package name describe what it holds, so a reader does not have to
  open it to learn its two members.
- State a membership rule that decides future adapter placement without
  repeating this discussion.
- Keep the change to a package and import rename.

**Non-Goals:**

- Introducing a layered adapter tree (`adapter/driving`, `adapter/driven`).
  That reorganizes `ai`, `scripted`, `evals`, `web`, and `demo`, mixes driving
  and driven sides in one subtree, and conflicts with the feature-first layout.
- Moving any state holder whose state carries an invariant.
- Extracting a `PanVault` port.
- Any behavior, wiring, configuration, or spec change.

## Decisions

### 1. Name the package `store`, not `persistence`

`persistence` promises durability the project explicitly does not have: the
README states it is "not a persistence or PCI story", and
`sensitive-data-handling` requires the vault mapping to stay in memory. The
README already calls these classes "in-memory stores", so `store` is both
accurate and consistent with existing wording.

Alternatives considered: keep `infrastructure` (keeps the misnomer; it is the
only layer-named package in the tree), `persistence` (overpromises durability),
`data` (too vague to guide placement).

### 2. Membership rule: replaceability is a design goal

A class belongs in `store` when swapping its implementation is a stated goal.
`DemoData` (seed today, database later) and `InMemoryRefundLedger` (in-memory
today, durable ledger later) qualify; the spec places no constraint on either.

The other state holders do not qualify:

- `policy.AuthorizationTokens` - `mint` is package-private so nothing outside
  `policy` can forge authority. Moving it would break that invariant.
- `security.PanVault` - the spec requires a session-scoped mapping held only in
  memory and forgotten on restart.
- `assistant.EscalationQueue` and `assistant.SessionAccounts` - application
  state whose lifetime is the conversation.

Consequence: "holds state" is not a reason to move a class here.

### 3. `PanVault` stays in `security`; no port is extracted

`PanVault` has two core consumers, `security.InputRedactor` and
`security.CardDirectory`. Moving the class alone would make the core depend on
the adapter package. Preserving the direction therefore requires a core-owned
port plus a renamed implementation (`persistence`/`store.InMemoryPanVault`),
touching about seven test files and adding an interface with one
implementation.

The interface would buy a swap the spec forbids (durable vault) or a
tokenization service that is out of scope for the demo. So the move is
deferred indefinitely, and the interface with it.

If it is ever revisited, the direction-preserving form is: keep `PanVault` as a
`security` interface, add only the implementation to `store` - mirroring
`assistant.port.out` + `observability.MicrometerTurnTelemetry`.

### 4. Rename only; leave the rest of the tree alone

`ai`, `observability`, and `scripted` are adaptive infrastructure in the
ordinary sense, but renaming or regrouping them would not answer the problem
this change addresses (one misleading package name) and would touch most of the
codebase. The feature-first layout already gives each adapter a home.

`PanVault.lastFour` and `PanVault.size` have no production callers. Trimming
them is an API-hygiene change, unrelated to package placement, and stays out to
keep this diff reviewable.

## Risks / Trade-offs

- **Missed references leave the build broken.** -> The rename is mechanical:
  `git mv` both classes and both tests, then grep for `refundguardrails.infrastructure`
  and `refundguardrails\.infrastructure` across `src`, and compile before testing.
- **Docs drift from code.** -> Update the README package table row and the
  mermaid node in the same commit; no other doc mentions the package.
- **Test packages must move with the classes.** -> `DemoDataTest` and
  `InMemoryRefundLedgerTest` live in the matching test package; move them so
  package-private access and test discovery stay intact.
- **The naming question returns for the next state holder.** -> Decision 2 is
  the answer; consider recording it in the README table in a later change if it
  keeps coming up.

## Migration Plan

1. `git mv` the two main classes and their two tests to the `store` package;
   update their `package` lines.
2. Update the three importing tests (`CardDirectoryTest`, `RefundPolicyTest`,
   `RefundServiceTest`).
3. Update the README table row and mermaid node.
4. `./gradlew test` then `./gradlew eval`; expect the same suite and cases as
   before with no assertion changes.

Rollback is a revert of the single commit; there is no data or deployment
migration, and Spring bean names are unchanged because the class names are.
