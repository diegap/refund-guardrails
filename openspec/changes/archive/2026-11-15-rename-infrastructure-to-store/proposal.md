# Proposal

## Why

`infrastructure` is the only layer-named package in a tree that is otherwise
organized feature-first, and it holds just two in-memory driven adapters. The
name promises a broad technical layer that does not exist, so a reader has to
open the package to learn it is only `DemoData` and `InMemoryRefundLedger`. The
name also overpromises durability that the project deliberately does not have
(see README: "it is not a persistence or PCI story").

## What Changes

- Rename the package `dev.diegap.refundguardrails.infrastructure` to
  `dev.diegap.refundguardrails.store`. Both classes (`DemoData`,
  `InMemoryRefundLedger`) and the two matching test classes move with it.
- Update the imports in the three tests that construct these adapters directly
  (`CardDirectoryTest`, `RefundPolicyTest`, `RefundServiceTest`).
- Update the README package table and the hexagonal-layout diagram node to use
  `store`.
- Do **not** move `PanVault`, `CardDirectory`, `EscalationQueue`,
  `SessionAccounts`, or `AuthorizationTokens`. Their state carries a stated
  invariant (in-memory vault, package-private token minting, session binding),
  so their replaceability is not a design goal.
- Do **not** extract an interface for `PanVault`. It has core consumers
  (`InputRedactor`, `CardDirectory`), so moving it would require a core-owned
  port; that cost buys nothing while the spec requires the mapping to stay
  in memory.
- No behavior, HTTP contract, configuration key, bean wiring, or spec changes.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is a package rename with no spec-level behavior change, so the
change sets `skip_specs: true`.

## Impact

- Code: `infrastructure` (2 classes) becomes `store`; imports in
  `security/CardDirectoryTest`, `policy/RefundPolicyTest`, and
  `policy/RefundServiceTest` change.
- Spring wiring: unchanged. Component scan still covers the same classes under
  `dev.diegap.refundguardrails`.
- Tests: package and import lines only; no assertions change.
- Docs: one README table row and one diagram node.
- No dependency, API, YAML, or configuration change.
- Follow-up left out on purpose: `PanVault.lastFour` and `PanVault.size` have no
  production callers and could be trimmed in a separate change.
