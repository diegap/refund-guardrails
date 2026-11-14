# Proposal

## Why

Most LLM-in-financial-services demos put sensitive data into the prompt and then
ask the model, in natural language, not to repeat it or to respect a spending
limit. That design fails the moment the model is persuaded, and it is impossible
to prove either way.

This change builds a small, fully inspectable reference implementation of the
opposite approach: the model never receives a card number, and it cannot
authorize a refund on its own. A deterministic policy service mints the
authorization, and a test suite proves both properties. The goal is a working,
weekend-scale lab that demonstrates the split between *probabilistic suggestion*
and *deterministic authority*.

## What Changes

- New Spring Boot / Spring AI application implementing a refund support agent
  with tool calling, in-memory domain data, and no external accounts required.
- New scripted LLM provider: an OpenAI-compatible endpoint served in-process, so
  the application always runs against the real `OpenAiChatModel` HTTP path
  without an API key, and so the same code can later point at a real provider by
  configuration alone.
- New sensitive-data handling: PAN detection (Luhn + BIN), vaulting, token
  substitution before the model call, and a `last4`-only projection returned by
  tools.
- New refund authorization: deterministic policy rules validate amount and
  charge eligibility, and only the policy service can mint a single-use
  authorization token that the `issue_refund` tool requires.
- New guardrail layer as Spring AI advisors: prompt-injection heuristic, scope
  refusal, and output PII scanning, layered in front of (not instead of) the
  structural controls.
- New evaluation suite: a small golden set of cases run as tests, deterministic
  assertions, and a report. Includes a privacy invariant asserted against the
  scripted provider's raw request body.
- New observability: Spring AI `gen_ai.*` spans exported over OTLP to a local
  viewer, with the backend left as configuration so a hosted backend can be
  enabled later without code changes.
- New single-page demo UI with scripted scenarios and a "what the model saw"
  panel that shows the redacted prompt next to the user's raw input.
- New public repository scaffolding: README stating the thesis and limitations,
  MIT license, `.gitignore`, `.env.example`, and a GitHub Actions workflow that
  builds and runs the evaluation suite.

Out of scope for this change: RAG, streaming responses, chat memory across
sessions, a real database, multi-user authentication, human-approval workflow,
real-provider validation, hosted deployment, and any hosted observability
account.

## Capabilities

### New Capabilities

- `scripted-llm-provider`: an OpenAI-compatible chat-completions endpoint served
  locally, with fixture-driven deterministic responses, scenario routing, and
  the ability to switch to a real provider through configuration.
- `sensitive-data-handling`: detection and vaulting of card numbers in user
  input, token substitution before model calls, `last4`-only projection for
  tools, output scanning, and the guarantee that no PAN reaches the model.
- `refund-authorization`: deterministic policy rules, policy-minted single-use
  authorization tokens, tool-level token validation, and escalation when a
  request exceeds policy.
- `refund-assistant`: the conversational entry point, tool set, guardrail
  advisors (injection heuristic, scope refusal, output PII), and the scripted
  demo scenarios that make the guarantees observable.
- `llm-evaluation`: the golden case set, deterministic assertion types, the
  evaluation runner, the privacy invariant, and the report consumed by CI.
- `llm-observability`: Spring AI observation spans with prompt and completion
  attributes, OTLP export to a local viewer, and backend-swap configuration.

### Modified Capabilities

None. This is a greenfield project with no existing specs.

## Impact

- Entirely new codebase; no existing behavior or APIs change.
- Runtime: Java 25, Spring Boot 4.x, Spring AI 2.0.x, Thymeleaf, Actuator,
  Micrometer Tracing / OpenTelemetry.
- Test and CI: JUnit 5, Maven, GitHub Actions on a public repository.
- Local tooling: one OTLP trace viewer installed via Homebrew (no Docker).
- External dependencies at runtime: none. No API keys, no cloud accounts, no
  containers.
- Data: documented test PANs only; no real cardholder data is stored or
  processed.
