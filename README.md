# refund-guardrails

An LLM refund support agent that never receives a card number, and cannot
authorize a refund on its own. The model proposes; a deterministic policy
service decides. Both properties are covered by a deterministic test suite.

> Demo only. Uses documented test card numbers. Stores no real card data and is
> not PCI-compliant.

**Status:** weekend-scale lab, in progress. The plan lives in `openspec/`.

## Stack

- Java 25, Gradle (Kotlin DSL), Spring Boot 4.1, Spring AI 2.0
- Thymeleaf + htmx for the single-page demo
- OpenTelemetry traces over OTLP, exported to a local viewer
- JUnit 5 for the deterministic eval suite

## Run it

```bash
./gradlew bootRun --args='--spring.profiles.active=mock'
```

No API key and no network access are required. The `mock` profile starts a
scripted, OpenAI-compatible chat-completions endpoint inside the application and
points the real Spring AI OpenAI client at it, so the provider code path,
tool-calling loop, and `gen_ai.*` telemetry spans all run for real.

To see traces, run a local OTLP viewer on the default ports before starting the
app:

```bash
otel-desktop-viewer --open-browser=false   # UI on http://localhost:8000
```

Traces are exported to `http://localhost:4318/v1/traces`. If no viewer is
running, export fails quietly and requests still succeed.

## Scripted provider

The provider lives in `dev.diegap.refundguardrails.scripted` and is registered
only when the `mock` profile is active; without the profile `/v1/chat/completions`
is not served.

- **Request shape:** OpenAI-style `messages`, `tools`, `model`; non-streaming
  only. `stream: true` returns a documented `streaming_unsupported` error.
- **Determinism:** a scenario is selected by a `[[scenario:NAME]]` marker found
  in the request messages, with a `default` fallback for unknown names. Fixture
  text contains no timestamps or random identifiers, so repeated requests are
  identical.
- **Tool loop:** a fixture can return a tool call; once the conversation carries
  a `tool` message, the fixture's follow-up answer closes the loop.
- **Inspection:** each received body is captured in memory, keyed by the
  `[[session:NAME]]` marker, and never logged or written to disk. Tests assert
  both properties.
- **Offline:** the suite passes with outbound network denied; only loopback is
  allowed.

Fixtures cover a duplicate charge, an over-cap refund, a policy-override
attempt, and an off-topic request.

## Card-number vault

The model never receives a card number. On the way in, `PanDetector` looks for
12–19 digits that also have a known issuer prefix and pass the Luhn checksum;
spaces and dashes are treated as separators, so a number that fails any check is
left alone. Each match is replaced with a stable per-session token such as
`[[CARD_1]]` before any model-bound content exists. The same card in the same
session keeps the same token.

Tools resolve a token through `CardDirectory`, which returns only the brand and
last four digits. There is no lookup path that returns a full number. On the way
out, model output is scanned again and card numbers and email addresses become
`[REDACTED_CARD]` / `[REDACTED_EMAIL]`.

The vault is a demo control with deliberate limits:

- It lives in memory and is scoped to one session; a restart forgets every
  mapping and tokens stop resolving.
- The seeded domain holds the documented test card numbers in memory so the demo
  has something to look up. Those are public test numbers only.
- It is not a secret manager and nothing is persisted, so there is no encryption
  story to point at. It is not a production control and not PCI-compliant. Do
  not put real card data in this application.


## Configuration

The default demo needs nothing set. Values are read from environment variables;
`.env` is not loaded automatically (see `.env.example`).

| Variable | Purpose |
| --- | --- |
| `OPENAI_API_KEY` | Real-provider credential. Defaults to a demo placeholder. |
| `SPRING_AI_OPENAI_BASE_URL` | Override the model endpoint. The scripted provider uses this server's own port. |
| `SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL` | Model name. Defaults to `gpt-4o-mini`. |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | OTLP/HTTP trace endpoint. Defaults to the local viewer. |

### Switching to a real provider

This path is **untested in this lab**: it needs a key, network access, and
provider spend. To try it, set `OPENAI_API_KEY`, optionally set
`SPRING_AI_OPENAI_BASE_URL` and `SPRING_AI_OPENAI_CHAT_OPTIONS_MODEL`, and start
without the `mock` profile. The guardrails, policy engine, and evals do not
depend on which provider answers.

To send traces to a hosted backend, set `OTEL_EXPORTER_OTLP_ENDPOINT`. For
authentication headers, override
`management.opentelemetry.tracing.export.otlp.headers` in configuration or set
`MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_HEADERS_<NAME>`.
