# Refund Assistant Specification

## Purpose

Defines the user-facing refund conversation: the tools the model may use, the
guardrails applied on the way in and out, and the scenarios that make the
system's guarantees visible.

## Requirements

### Requirement: Conversational turn

The system SHALL accept a user message within a session and return an assistant
response together with turn metadata covering which guardrails evaluated it,
which tools ran, and which redactions were applied.

#### Scenario: Normal turn returns a response and metadata

- **WHEN** a user submits a message in scope
- **THEN** the response contains assistant text and metadata naming the
  guardrails evaluated, tools invoked, and redactions applied

#### Scenario: Turn state is scoped to the session

- **GIVEN** two sessions
- **WHEN** turns alternate between them
- **THEN** each response reflects only its own session's account, charges, and
  vault tokens

### Requirement: Tool surface

The assistant SHALL have access to exactly four tools — account lookup, charge
lookup, refund issuance, and human escalation — and none of them SHALL return a
full card number.

#### Scenario: Charge lookup returns safe fields

- **WHEN** the assistant looks up charges
- **THEN** each charge exposes identifier, amount, merchant, date, status, and
  card last four only

#### Scenario: Unknown tool is not available

- **WHEN** a conversation attempts to invoke a tool outside the declared set
- **THEN** the invocation fails and no state changes

### Requirement: Input guardrails

The system SHALL evaluate input guardrails in order — card-number redaction,
prompt-injection heuristic, topic scope — and SHALL refuse the turn when the
injection or scope guard fails, without executing any tool.

#### Scenario: Injection attempt is refused

- **WHEN** input attempts to override system instructions, assume a privileged
  role, or demand an exception to policy
- **THEN** the turn is refused, no tool runs, and the guard failure is recorded

#### Scenario: Off-topic request is refused

- **WHEN** input is unrelated to accounts, charges, refunds, or escalation
- **THEN** the response declines and redirects to the supported scope

#### Scenario: Legitimate refund request passes

- **WHEN** input asks about a charge and a refund within policy
- **THEN** all input guardrails pass and the turn proceeds

### Requirement: Guardrails do not replace authorization

A turn in which an input guardrail fails to detect an attack SHALL still be
unable to produce an unauthorized refund.

#### Scenario: Guard miss still cannot move money

- **GIVEN** an adversarial input that passes the injection heuristic
- **WHEN** the model requests a refund above the cap
- **THEN** policy refuses authorization and the refund ledger is unchanged

### Requirement: Output scanning before delivery

Assistant output SHALL pass through output scanning before it is returned to the
user.

#### Scenario: Sensitive pattern in output is redacted

- **WHEN** model output contains a card number or email address
- **THEN** the delivered text contains a redaction marker instead

### Requirement: Model-bound content is inspectable

For each turn, the system SHALL make the exact content sent to the model
available to the interface, in redacted form, alongside the user's raw input.

#### Scenario: Redaction panel shows the difference

- **WHEN** a turn containing a card number completes
- **THEN** the interface can display the raw input and the model-bound content
  side by side, with the token visible in the latter

### Requirement: Scripted demonstration scenarios

The system SHALL provide four named, deterministic scenarios — duplicate charge,
over-cap refund, policy-override attempt, and off-topic request — each runnable
in one action and each exhibiting its intended guarantee.

#### Scenario: Duplicate charge scenario succeeds

- **WHEN** the duplicate charge scenario runs
- **THEN** a refund within policy completes and the trace shows the redaction

#### Scenario: Over-cap scenario is refused by policy

- **WHEN** the over-cap scenario runs
- **THEN** the guardrail or policy refuses the refund and escalation is offered

#### Scenario: Policy-override scenario demonstrates defense in depth

- **WHEN** the policy-override scenario runs
- **THEN** the outcome is an explanation and escalation, with no refund recorded

#### Scenario: Off-topic scenario is declined

- **WHEN** the off-topic scenario runs
- **THEN** the response declines and does not call a tool

### Requirement: Safe failure

Provider or tool failures SHALL produce a user-safe message and SHALL NOT expose
stack traces, internal identifiers, card data, or configuration.

#### Scenario: Provider failure is contained

- **WHEN** the model provider returns an error or is unreachable
- **THEN** the user receives a safe failure message and the turn metadata records
  the failure category
