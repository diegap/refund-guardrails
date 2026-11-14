# Tasks

Weekend budget: ~12 hours. Tasks are ordered by dependency, and the two spikes
in group 2 run before any feature work because they can invalidate the
observability approach. Effort hints are rough.

## 1. Repository and build foundation

- [x] 1.1 Create an empty public repository on GitHub (no README, no license, no
  `.gitignore`), then initialize the local repository on `main` and add the
  `origin` remote. Verify `git remote -v` shows the expected SSH URL and an
  empty first push succeeds. (~15m)
- [x] 1.2 Generate the Spring Boot 4 / Spring AI 2.0 Gradle project (Kotlin DSL)
  with web, actuator, Thymeleaf, and the OpenAI starter, pinned to the current
  GA versions, and commit the wrapper (`gradlew`, `gradlew.bat`,
  `gradle/wrapper/`). Verify `./gradlew build` succeeds and
  `./gradlew bootRun` starts and serves a placeholder page. (~30m)
- [x] 1.3 Add repository hygiene: `.gitignore` for build output, IDE files,
  `.env`, and the local trace database; MIT `LICENSE`. Verify `git status` stays
  clean after a build and no credential values appear in any tracked file. (~15m)
- [x] 1.4 Write the README skeleton: one-line thesis, the demo-only/no-real-card-
  data disclaimer, the quickstart command, and empty sections for the remaining
  docs. Verify the quickstart command as written starts the application. (~20m)
- [ ] 1.5 Add a CI workflow that runs the build and tests on push and pull
  request on a public repository, using `actions/setup-java` (Temurin 25) and
  `gradle/actions/setup-gradle` for caching, running `./gradlew build`. Verify
  the workflow is green on the first push and the job log shows the test phase
  executing. (~15m)

## 2. Risk-retiring spikes

- [ ] 2.1 Install a local OTLP trace viewer via Homebrew (no container runtime)
  and confirm its receiver answers on the documented ports. Verify a test span
  sent with a trivial client appears in the viewer. (~15m)
- [ ] 2.2 Spike the scripted endpoint: serve a minimal chat-completions response,
  point the provider base URL at it, and complete one turn. Verify the trace
  contains a model-call span with provider and model attributes, proving a
  hand-written mock model would have been the wrong choice. (~45m)
- [ ] 2.3 Spike prompt/completion content attributes on Spring AI 2.0 using an
  observation filter. Verify either that redacted content appears on the span or
  record the documented fallback (metadata-only spans) before continuing. (~45m)
- [ ] 2.4 Spike span hygiene: suppress duplicate HTTP observations and confirm
  that one turn produces one turn span and one model-call span, with the
  internal scripted request not appearing as a second conversation. (~20m)
- [ ] 2.5 Record the spike outcomes in the design document's risk section,
  including any deviation from the planned approach. Verify the document matches
  what was actually observed. (~15m)

## 3. Scripted LLM provider

- [ ] 3.1 Implement the non-streaming chat-completions endpoint, registered only
  under the mock profile, returning a valid payload with choices, message, and
  usage; reject `stream: true` with a documented error. Verify tests cover the
  well-formed response, the streaming rejection, and the endpoint's absence when
  the profile is inactive. (~45m)
- [ ] 3.2 Implement fixture selection by explicit scenario marker with a default
  fallback, and record the selection per request. Verify tests show identical
  responses for repeated requests, fallback for an unknown marker, and no
  interference between two concurrent sessions. (~45m)
- [ ] 3.3 Author fixtures for the four scenarios, including at least one scripted
  tool call with schema-valid JSON arguments. Verify an integration test drives
  the tool loop to completion and a malformed-arguments fixture fails loudly.
  (~60m)
- [ ] 3.4 Capture each received request body in memory, keyed by session, and
  expose it for inspection. Verify a test retrieves the captured body, and a
  shutdown test confirms nothing is written to disk or logs. (~30m)
- [ ] 3.5 Verify offline operation: start with no environment variables and no
  network access, then complete a chat turn. Verify the test passes with network
  disabled. (~15m)
- [ ] 3.6 Document the scripted provider and the provider-switch configuration in
  the README, and add `.env.example` with placeholders only, labelling
  real-provider use as untested. Verify the documented configuration keys match
  the application config. (~20m)

## 4. Domain model and sensitive-data handling

- [ ] 4.1 Implement the in-memory domain: three accounts seeded with documented
  test PANs, a handful of charges, and a refund ledger. Verify a seed test
  asserts the expected accounts, charges, and amounts, and that every PAN used is
  an industry test number. (~30m)
- [ ] 4.2 Implement card-number detection using length, issuer prefix, and Luhn,
  normalizing spaces and dashes. Verify unit tests cover a formatted card number,
  a number that fails Luhn, and an order identifier that must pass through
  unchanged. (~45m)
- [ ] 4.3 Implement the input advisor that replaces detected numbers with
  session-scoped tokens before any model-bound content exists. Verify tests for
  token substitution, token reuse within a session, isolation across sessions,
  and vault loss on restart. (~45m)
- [ ] 4.4 Restrict card lookups to a last-four projection and ensure tool results
  never carry a full number. Verify a test asserts that no tool result contains
  more than four consecutive digits of any seeded card number. (~30m)
- [ ] 4.5 Implement output scanning and redaction for card numbers and email
  addresses before delivery. Verify tests for a leaking model output and for an
  unchanged clean output. (~30m)
- [ ] 4.6 Include redaction metadata in each turn result: stage, token, and
  visible last four. Verify the turn endpoint returns it for a turn containing a
  card number. (~20m)
- [ ] 4.7 Document the vault design and its limitations (in-memory, per session,
  demo only) in the README. Verify the wording matches the tested behavior and
  makes no PCI-compliance claim. (~15m)

## 5. Refund authorization

- [ ] 5.1 Implement policy evaluation: positive amount, not exceeding the charge,
  charge belongs to the account and is refundable, and amount within the
  configured auto-approval cap. Verify one unit test per rule, including a
  denied case and an allowed case. (~45m)
- [ ] 5.2 Implement authorization tokens: minted only on policy success, bound
  to charge, amount, and session, short-lived and single-use. Verify tests for
  successful minting, reuse rejection, expiry rejection, and amount mismatch.
  (~45m)
- [ ] 5.3 Enforce the token in the refund tool and apply the ledger change only
  on success. Verify a happy-path test records exactly one refund, and rejection
  tests leave the ledger unchanged for missing, forged, mismatched, and expired
  tokens. (~45m)
- [ ] 5.4 Implement the denial path: reason category without sensitive detail,
  no token, and an escalation offer. Verify a test asserts the refusal text,
  absence of a token, and presence of the escalation option. (~20m)
- [ ] 5.5 Add the defense-in-depth test: an adversarial turn that bypasses the
  injection heuristic still cannot produce an over-cap refund. Verify the test
  fails if token validation is removed. (~30m)
- [ ] 5.6 Implement double-refund prevention for fully refunded charges. Verify a
  test rejects a second refund on the same charge. (~20m)
- [ ] 5.7 Document the "model proposes, policy authorizes" flow with a diagram in
  the README. Verify the diagram matches the implemented tool and token flow.
  (~20m)

## 6. Assistant and guardrails

- [ ] 6.1 Build the chat service around `ChatClient` with an explicit advisor
  order: redaction, injection, scope on input; scanning on output. Verify an
  order test proves redaction runs before any content can be observed or
  forwarded. (~45m)
- [ ] 6.2 Implement the prompt-injection heuristic and the scope guard. Verify
  tests for a refused injection with no tool executed, a refused off-topic
  request, and a legitimate request passing every guard. (~45m)
- [ ] 6.3 Define exactly four tools — account lookup, charge lookup, refund
  issuance, human escalation — with schemas the model can call. Verify a test
  asserts the declared tool set and that an unknown tool cannot be invoked.
  (~45m)
- [ ] 6.4 Return per-turn metadata covering guards evaluated, tools invoked, and
  redactions applied, and expose the model-bound content for the interface.
  Verify a test asserts metadata and redacted model input for a turn. (~30m)
- [ ] 6.5 Implement safe failure handling for provider and tool errors. Verify a
  test asserts a safe user message, no stack trace, and a failure category in
  metadata. (~20m)
- [ ] 6.6 Wire the four named scenarios to deterministic fixtures. Verify one
  test per scenario asserting its intended outcome, and confirm the
  policy-override scenario demonstrates a guard miss contained by policy. (~45m)
- [ ] 6.7 Document the guardrail layers and the defense-in-depth rationale in the
  README. Verify the documented behavior names the guard that is intentionally
  imperfect and why that is safe. (~20m)

## 7. Demo interface

- [ ] 7.1 Build the single chat page with Thymeleaf and htmx, including the four
  scenario buttons. Verify the page renders, a turn updates without a full
  reload, and no Node build step is required. (~60m)
- [ ] 7.2 Add the "what the model saw" panel showing raw input and redacted
  model-bound content side by side. Verify a scenario demonstrates the token in
  the panel where the raw input shows the card number. (~30m)
- [ ] 7.3 Add a screenshot or short recording of the scenario flow to the README.
  Verify a reader can reproduce the pictured state using the documented steps.
  (~20m)

## 8. Observability

- [ ] 8.1 Configure OpenTelemetry export and ensure each turn emits a root span
  with a session identifier plus a model-call child span with provider, model,
  and token usage. Verify by inspecting a turn in the local viewer. (~45m)
- [ ] 8.2 Record each guardrail decision on the trace with guard name, outcome,
  and reason category. Verify a refused turn's trace identifies the failing
  guard without exposing content. (~30m)
- [ ] 8.3 Make prompt/completion content attributes opt-in by configuration,
  supporting disabled-by-default and redacted-only behavior. Verify tests for
  both modes and that enabled content shows the token, not the card. (~30m)
- [ ] 8.4 Write the tracing documentation: local viewer setup, export endpoint,
  and the environment variables for pointing at a different backend later.
  Verify the documented steps work from a clean shell. (~20m)
- [ ] 8.5 Add a telemetry hygiene test asserting that exported spans carry no
  credential values and no full card number. Verify the test fails if a PAN is
  placed on a span. (~20m)

## 9. Evaluation suite

- [ ] 9.1 Define the declarative case format and loader. Verify adding a case to
  the case file runs it with no code change. (~30m)
- [ ] 9.2 Implement the runner with fresh session, vault, and ledger state per
  case. Verify an isolation test where two sequential cases cannot observe each
  other's state. (~45m)
- [ ] 9.3 Implement the assertion types: guard fired, refusal, tool sequence,
  contains, omits, no PAN in model-bound request, ledger unchanged. Verify each
  assertion has both a passing and a failing test. (~60m)
- [ ] 9.4 Apply the privacy invariant to every case regardless of declared
  assertions. Verify a deliberately leaking case fails with a privacy violation.
  (~20m)
- [ ] 9.5 Author the golden set: around twelve cases covering the four scenarios,
  guardrail edges, and authorization edges. Verify all cases pass and none
  depend on wall-clock time or network. (~45m)
- [ ] 9.6 Emit JSON and Markdown reports with per-case outcomes, totals, and the
  suite-scope statement. Verify the report files contain the required fields and
  the scope statement. (~30m)
- [ ] 9.7 Add a command-line entry point that runs the suite and exits non-zero on
  failure. Verify exit codes for an all-pass run and a failing case. (~20m)
- [ ] 9.8 Extend CI to run the suite and publish both reports as artifacts.
  Verify a deliberately failing case fails the job, then revert it. (~20m)

## 10. Integration and release readiness

- [ ] 10.1 Run the full system from a clean clone with an empty environment.
  Verify the README quickstart works verbatim and every scenario produces its
  documented outcome. (~30m)
- [ ] 10.2 Complete the README: architecture diagram, design decisions, the
  evaluation report excerpt, explicit limitations, and the untested-provider
  statement. Verify every claim is backed by a test or an observed run. (~45m)
- [ ] 10.3 Review the public history for leaked data. Verify `git log -p` and a
  tracked-file scan find no credential values and no non-test card numbers.
  (~20m)
- [ ] 10.4 Confirm the change is ready for review: `openspec validate` passes and
  every spec requirement has a corresponding test or a ticket-style follow-up.
  Verify the validation command's output. (~15m)

## Workflow follow-up

- Archive this change once the repository is public, the evaluation suite is
  green in CI, and the README claims match observed behavior.
- Verify the archived specs and the main specs agree after archiving.
