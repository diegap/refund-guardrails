# Design

## Context

Greenfield project; see `proposal.md` for motivation. The constraints that shape
this design:

- **Weekend budget** (~12 hours of work), so every decision is judged by
  "what does it buy per hour".
- **No API key, no cloud account, no Docker** on the development machine. Java
  25, Gradle 9.8 with a committed wrapper, Node 26 are available; there is no
  container runtime.
- **Public repository from the first commit**, so fixtures must be obviously
  fake and the README must state limits.
- Spring AI 2.0.x requires Spring Boot 4.x (Framework 7, Jackson 3).

## Goals / Non-Goals

**Goals:**

- Demonstrate, in running code, that a probabilistic component can be wrapped by
  deterministic controls that hold even when the model is adversarial.
- Keep the entire system runnable with a single `./gradlew bootRun` and zero
  external services.
- Make the guarantees observable (traces, "what the model saw" panel) and
  testable (evaluation suite) rather than asserted in prose.
- Leave clean seams for three later additions without over-building now: a real
  model provider, a hosted trace backend, and judge-based evaluation.

**Non-Goals:**

- Production characteristics: persistence, authentication, multi-tenancy,
  rate limiting, PCI scope, real provider fidelity.
- Agentic breadth: planning loops, multi-agent orchestration, RAG, vector
  stores, MCP.
- A polished product UI.

## Decisions

### D1: Spring AI 2.0 on Spring Boot 4

**Chosen** because the project is already a Spring application: the integration
tax is near zero, the `ChatClient` advisor chain is exactly the interception
point guardrails need, Actuator + Micrometer + OpenTelemetry come pre-wired, and
`Evaluator` types give a Java-native evaluation seam. It is also the framework a
Java/Spring audience will recognise.

**Alternatives:** LangChain4j (lighter, provider-agnostic, ships a guardrails
module, but observability and evaluation are hand-wired and the Spring
integration adds a second abstraction); raw `openai-java` SDK or plain
`HttpClient` (maximum learning about the wire protocol, but guardrails, tool
loop, tracing and evals all become DIY — impossible in a weekend).

**Consequence:** version pinning matters. Spring Boot 4 / Spring AI 2.0 is a
young combination; generate the project from Initializr rather than copying a
blog post written against 1.0.x.

### D2: Scripted OpenAI-compatible provider, not a mock `ChatModel`

**Chosen:** serve a fake `POST /v1/chat/completions` from the application itself
and point `spring.ai.openai.base-url` at it. The real `OpenAiChatModel` is used,
so the HTTP path, serialization, retry behavior and `gen_ai.*` observation spans
are all genuine. Three benefits beyond fidelity: the provider can emit
`tool_calls` so the tool loop is exercised, it can be the privacy oracle (it
records the exact request body the model received), and switching to a real
provider is a base-url and API-key change.

**Alternatives:** a hand-written `MockChatModel` bean (30 minutes, but bypasses
the provider client and risks missing the observation spans the project is
about); record/replay cassettes (high fidelity, requires one real run to seed —
impossible without a key).

**Consequence:** the fake must be believable enough to route fixtures. Routing
is by an explicit scenario marker carried in the request (for example a header
or a system message convention), with a deterministic default fixture for
unmatched requests. Streaming is rejected explicitly rather than half-supported.

### D3: Deterministic authorization, not prompt-level policy

**Chosen:** the model proposes `issue_refund(chargeId, amount)`; a policy service
evaluates the rules and, on success, mints a single-use token bound to
(charge, amount, session). The tool executes only with a valid token. The model
cannot widen its own authority because the limit lives in server configuration
and the token is unguessable.

**Alternatives:** instruct the model in the system prompt to respect the cap
(fails under injection, unprovable); validate only in the tool but let the model
supply the amount threshold (still model-controlled).

**Consequence:** the demo needs a scenario where the input guard *fails* and the
system still holds. That is the point of the design, not an embarrassment.

### D4: PAN vault with token substitution

**Chosen:** detect PANs in user input using length + prefix + Luhn (Luhn alone
produces too many false positives on order numbers), replace each with a stable
per-session token such as `[[CARD_1]]`, and keep the mapping in memory. Tools
resolve tokens to a `last4` projection. The model-bound messages therefore
contain tokens, never PANs.

**Alternatives:** masking (`**** **** **** 4242`) — leaks the last four to the
model and is not reversible, so tools cannot look up the card; hashing — same
irreversibility problem; trusting the model not to echo the PAN — not a control.

**Consequence:** the vault is per-session and in-memory; a restart forgets it.
That is acceptable for a demo and is stated as a limitation. The `last4`
projection is the deliberate safe value: it is what a support agent is allowed
to see.

### D5: Guardrails as advisors, layered in front of structural controls

**Chosen:** input advisors (PAN redaction first, then injection heuristic, then
scope) and output advisors (PII scan), all registered on the `ChatClient`
advisor chain. Ordering matters: redaction must run before anything logs or
forwards content.

**Alternatives:** a single monolithic "guard chain" (harder to demonstrate
individually); framework-feature guardrails alone (they do not cover vaulting or
authorization, which are the interesting parts).

**Consequence:** the heuristic injection guard is intentionally imperfect and is
documented as such. Its role is fast, explainable rejection; D3 is what actually
holds.

### D6: Deterministic evals now, judge-based evals later

**Chosen:** a declarative YAML case set executed by a runner that resets state
per case, asserts on observable outcomes, and emits JSON + Markdown. Assertion
types: guard fired, refusal, tool sequence, response contains/omits, no PAN in
the model-bound request, refund state unchanged.

**Alternatives:** LLM-as-judge (requires a real key and a second model; noisy
without calibration); Postman/HTTP smoke tests (do not see internal state).

**Consequence:** the suite tests the plumbing, guardrails and policy engine — not
model quality. The README and the report must say so explicitly. This is a
feature: the suite is deterministic and runs offline in CI.

### D7: Local OTLP viewer now, hosted backend by configuration

**Chosen:** Spring AI observations export over OTLP to a local single-binary
viewer (no Docker), via `spring-boot-starter-opentelemetry` and
`management.opentelemetry.tracing.export.otlp.*`. Endpoint and headers are
configuration properties, so a hosted backend is a property or environment
change. Export failures must not fail requests.

**Alternatives:** defer tracing entirely (loses the "inspectable" property, and
the trace becomes unverifiable); self-host a full observability stack (needs
Docker).

**Consequence:** the local viewer shows spans and attributes but not a
generation-centric view. The application's own "what the model saw" panel fills
that gap; the two together are more convincing than a screenshot of a hosted
dashboard.

### D8: Thymeleaf + htmx, one deployable

**Chosen:** server-rendered chat page with htmx for partial updates; no Node
build, no separate frontend, one port to expose. The page includes the four
scenario buttons and a redaction panel.

**Alternatives:** React/Vite SPA (two build systems for one text box);
curl-only demo (weak for a public repo).

### D9: In-memory domain state, seeded deterministically

**Chosen:** three fake accounts with documented test PANs, a handful of charges,
and a refund ledger in memory. Deterministic seeds make evals reproducible.

**Alternatives:** H2/SQLite (persistence buys nothing for a one-session demo);
Testcontainers (no container runtime).

### D10: Public-repository hygiene as a design constraint

**Chosen:** documented test PANs only, a disclaimer at the top of the README, MIT
license, `.gitignore` covering build output, IDE files, `.env` and the local trace
database, and `.env.example` with placeholders only. Implementation artifacts
must not contain career or personal framing.

**Consequence:** no secrets exist in v1 at all, which is also why the demo can be
cloned and run by anyone.

## Architecture

```
+---------------------------+
| browser: chat + scenarios |
| chat panel | redaction    |
+-------------+-------------+
              | htmx POST /chat
              v
+---------------------------+        +----------------------+
| refund-assistant          |        | PAN vault (in-memory)|
|  input advisors:          |<------>|  [[CARD_n]] <-> PAN  |
|   redact PAN -> inject -> |        |  last4 projection    |
|   scope                   |        +----------------------+
|  ChatClient (tool loop)   |
|  output advisor: PII scan |
+------+---------------+----+
       |               |
       | tools         | model call
       v               v
+--------------+   +--------------------------+
| policy       |   | scripted provider        |
| service      |   | /v1/chat/completions     |
| mint/verify  |   | fixtures + tool_calls    |
| auth token   |   | records raw request body |
+--------------+   +--------------------------+
       |
       v
+---------------------------+
| refund ledger (in-memory) |
+---------------------------+

        everything above emits OTLP spans
                     |
                     v
        local trace viewer  (later: hosted backend)
```

Package sketch (single Gradle module):

```
dev.<user>.refundguardrails
  assistant/     chat service, advisors, tools, scenarios
  security/      pan detection, vault, pii scan, injection heuristic
  policy/        rules, authorization tokens, ledger
  provider/      scripted chat-completions controller, fixtures
  domain/        accounts, charges, refunds
  eval/          case model, runner, assertions, report writer
  web/           controllers, templates
```

## Spike Outcomes

Verified on 2026-11-14 against Spring Boot 4.1.2, Spring AI 2.0.1 and
`otel-desktop-viewer` 0.7.0:

| Spike | Outcome |
| --- | --- |
| 2.1 local viewer | Viewer answers on OTLP/HTTP 4318, gRPC 4317 and UI 8000; an OTLP/JSON span posted with `curl` appears immediately. |
| 2.2 scripted endpoint | A `/v1/chat/completions` stub driven through the real `OpenAiChatModel` produces `gen_ai.operation.name`, `gen_ai.system`, request/response model and token-usage attributes. Spring AI 2.0 uses the official OpenAI Java SDK, so `base-url` must include `/v1`. |
| 2.3 content attributes | Spring AI's `log-prompt` / `log-completion` handlers only write to the application log. A `@Component ObservationFilter` is still required to attach `gen_ai.prompt` and `gen_ai.completion` to the model span; Boot 4 registers filter beans automatically. |
| 2.4 span hygiene | One turn yields four spans: `spring_ai chat_client` → `tool _calling` → `call` → `chat gpt-4o-mini`. The internal scripted request is suppressed by disabling `http.server.requests` and `okhttp.requests`; the second key is the one that matters, because Spring AI's `SpringAiOpenAiHttpClient` emits an OkHttp observation, not `http.client.requests`. |

### Group 3 verification

- **Memory-only capture (3.4):** `ScriptedCaptureMemoryTest` points
  `java.io.tmpdir` at a fresh directory, sends a unique marker in the request
  body, and asserts the marker appears in neither the log events nor any file
  under that directory. The capture component holds records in memory only.
- **Offline operation (3.5):** the 13 scripted-provider tests pass under
  `sandbox-exec` with all outbound network denied and only `localhost` allowed.
  Gradle also runs `--offline`, so no dependency resolution needs the network.
  The provider URL is derived from `server.port`, so the test proves the default
  configuration points at the in-process server rather than an external host.
- **Trace export is best-effort:** with the OTLP endpoint pointed at a closed
  port (`http://localhost:9/v1/traces`), a chat turn still completes and the
  test passes; export failures are logged asynchronously, not surfaced to the
  caller.
- **Real provider untested:** the switch keys are documented and wired, but the
  lab has no key and does not exercise an external provider.


## Risks / Trade-offs

- **Self-hosted fake endpoint could loop or shadow real config.** → bind the fake
  to the same port and derive base-url from the runtime port; guard with an
  explicit `mock` profile that must be active for the endpoint to be registered.
- **Spring AI 2.0 observation API may differ from the published Langfuse guide
  (written for 1.0.9).** → resolved by spike 2.3: the filter still works, with
  the same attribute names, and is registered as a plain bean.
- **Trace noise from the fake provider's own HTTP server span.** → resolved by
  spike 2.4 by disabling `http.server.requests` and `okhttp.requests` in the
  `mock` profile.
- **Fixture authoring cost for tool calls.** → cap at three tools and four
  scenarios; if a fixture proves fiddly, script the response as a fixed
  multi-turn sequence rather than trying to be clever.
- **Luhn false positives on order numbers.** → require a plausible BIN prefix and
  keep a regression case; do not run the detector on tool inputs.
- **Scope creep.** → the cut list is explicit in `tasks.md`: streaming, memory,
  RAG, containers, pages, judge evals.
- **Initializr's Gradle generation returned HTTP 500 during setup.** → the project
  was assembled from Initializr's Maven output (which pins Boot 4.1.2 and Spring
  AI 2.0.1) plus a hand-written Kotlin DSL build; the wrapper is committed and
  `./gradlew build` passes.
- **A demo that requires no key can look unsophisticated.** → state plainly that
  responses are scripted and that the provider path is wired but untested;
  honesty is what makes the rest credible.
- **Public repo handling PAN-shaped strings.** → test PANs only, disclaimer,
  never echo raw input into logs.

## Migration Plan

Greenfield: no data or API migration. Rollout is a single initial push to a new
public repository. If the weekend runs short, the minimum shippable slice is:
scripted provider + PAN vault + policy authorization + one guardrail + README,
with evals marked as the next task — the repository must never be left in a
state where the README claims something the code does not do.

## Open Questions

- Whether the UI renders vault tokens verbatim (`[[CARD_1]]`) or decorates them
  as "card ending 4242" for readability. Cosmetic; either satisfies the specs,
  and the redaction panel shows the truth in both cases.
