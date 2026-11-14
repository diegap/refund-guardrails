# Spec Delta

## Purpose

Makes every model interaction and every guardrail decision inspectable locally,
and portable to a hosted trace backend by configuration alone.

## ADDED Requirements

### Requirement: Model call spans

Each model call SHALL emit a span carrying the operation name, provider, model
name, and token usage, and SHALL be a child of the turn's span.

#### Scenario: A completed turn produces model spans

- **WHEN** a chat turn completes
- **THEN** its trace contains at least one model-call span with provider, model,
  and token usage attributes

#### Scenario: Tool calls appear in the trace

- **WHEN** a turn invokes a tool
- **THEN** the trace shows the tool invocation beneath the turn

### Requirement: Content attributes are opt-in and redacted

The system SHALL support attaching prompt and completion content to model spans,
SHALL make that opt-in by configuration, and SHALL only attach redacted content.

#### Scenario: Content disabled by default

- **GIVEN** default configuration
- **WHEN** a model call completes
- **THEN** spans carry metadata and usage but no prompt or completion text

#### Scenario: Content enabled shows the token, not the card

- **GIVEN** content capture enabled
- **WHEN** a turn that contained a card number completes
- **THEN** the span content contains the vault token and no full card number

### Requirement: Guardrail decisions are traced

Each guardrail evaluation SHALL be recorded on the turn's trace with the guard
name, its outcome, and a reason category, so a refused turn is explainable from
the trace alone.

#### Scenario: Refusal is explainable from the trace

- **WHEN** a turn is refused by an input guardrail
- **THEN** the trace identifies which guardrail failed and its reason category

### Requirement: Export is configured by environment

Trace export SHALL be configured through standard OpenTelemetry environment
variables, SHALL require no code change to change destination, and SHALL be safe
to leave unconfigured.

#### Scenario: Unreachable collector does not break the application

- **GIVEN** a configured collector endpoint that is not reachable
- **WHEN** chat turns are served
- **THEN** responses are still returned successfully

#### Scenario: Destination changes without a rebuild

- **WHEN** the endpoint and headers are changed in the environment and the
  application restarts
- **THEN** spans are exported to the new destination

### Requirement: Local inspection is documented

The repository SHALL document how to run a local OpenTelemetry trace viewer and
which endpoint the application exports to, requiring no container runtime.

#### Scenario: A reader can view traces

- **GIVEN** the documented setup steps
- **WHEN** a reader follows them and runs a scenario
- **THEN** that scenario's spans are visible in the local viewer

### Requirement: Telemetry contains no secrets or card numbers

Spans, span events, and exported attributes SHALL NOT contain API keys,
authorization headers, session secrets, or full card numbers.

#### Scenario: No secret material is exported

- **WHEN** a trace is inspected after a turn
- **THEN** it contains no credential value and no full card number

### Requirement: Turns are correlatable

Each turn SHALL carry a session identifier on its root span so that the turns of
one conversation can be grouped without inspecting message content.

#### Scenario: Session grouping

- **GIVEN** three turns in one session
- **WHEN** the trace data is inspected
- **THEN** all three share the same session identifier
