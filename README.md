# refund-guardrails

An LLM refund support agent that never receives a card number, and cannot
authorize a refund on its own. The model proposes; a deterministic policy
service decides. Both properties are covered by a deterministic test suite.

> Demo only. Uses documented test card numbers. Stores no real card data and is
> not PCI-compliant.

**Status:** weekend-scale lab, in progress. The plan lives in `openspec/`.
Run with `./gradlew bootRun`; no API key required.