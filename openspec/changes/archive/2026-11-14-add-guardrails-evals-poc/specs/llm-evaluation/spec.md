# Spec Delta

## Purpose

Turns the system's safety and policy guarantees into a deterministic,
offline-runnable suite with a report that can gate changes in CI.

## ADDED Requirements

### Requirement: Declarative case format

Each evaluation case SHALL be declared as data, containing a name, an input
message, a scenario selection, and one or more expected assertions, and each case
SHALL run with fresh session, vault, and refund state.

#### Scenario: Case definition is data, not code

- **WHEN** a new case is added by editing the case file
- **THEN** the runner executes it without any code change

#### Scenario: Cases are isolated

- **WHEN** two cases run in sequence
- **THEN** neither observes state, vault tokens, or refunds produced by the other

### Requirement: Deterministic assertion types

The suite SHALL support at minimum these assertion types: a named guardrail
fired, the response is a refusal, the tool invocation sequence matches an
expectation, the response contains a string, the response omits a string, no card
number appears in the model-bound request, and the refund ledger is unchanged.

#### Scenario: Guardrail assertion

- **WHEN** a case expects the injection guard to fire
- **THEN** the case fails unless that guard was evaluated and failed

#### Scenario: Tool sequence assertion

- **WHEN** a case expects a specific ordered sequence of tool calls
- **THEN** the case fails if the sequence differs

#### Scenario: Ledger assertion

- **WHEN** a case expects no refund to be recorded
- **THEN** the case fails if the ledger gained an entry

### Requirement: Privacy invariant applies to every case

Every case SHALL assert that the model-bound request and the emitted telemetry
contain no full card number, independent of whether the case declares that
assertion.

#### Scenario: Leak fails an unrelated case

- **GIVEN** a case whose declared assertions all pass
- **WHEN** its model-bound request contains a full card number
- **THEN** the case is reported as failed with a privacy violation

### Requirement: Headless runner

The suite SHALL run without network access, SHALL be invocable from the test
lifecycle and as a command-line entry point, and SHALL exit non-zero when any case
fails.

#### Scenario: All cases pass

- **WHEN** the suite runs against a correct build
- **THEN** it exits zero and reports every case as passed

#### Scenario: One case fails

- **WHEN** any assertion fails
- **THEN** the runner exits non-zero and names the failing case and assertion

#### Scenario: Offline execution

- **GIVEN** no network access
- **WHEN** the suite runs
- **THEN** it completes successfully

### Requirement: Machine-readable and human-readable report

The suite SHALL write a JSON report and a Markdown report for each run, both
containing per-case outcomes and aggregate counts, and SHALL write them beneath
the build output directory.

#### Scenario: Report contents

- **WHEN** a run completes
- **THEN** the JSON report contains, per case, its name, outcome, and failed
  assertions, and the Markdown report contains the same information in a table
  with totals

### Requirement: Continuous integration gate

The repository SHALL run the build and the evaluation suite in continuous
integration on every push and pull request, SHALL fail the job when the suite
fails, and SHALL publish the reports as job artifacts.

#### Scenario: Failing evaluation fails the build

- **WHEN** a pull request makes a case fail
- **THEN** the continuous integration job fails

#### Scenario: Reports are retrievable

- **WHEN** the job completes
- **THEN** the JSON and Markdown reports are available as artifacts

### Requirement: Suite scope is stated, not implied

The reports and the repository documentation SHALL state that the suite validates
guardrails, policy behavior, and plumbing against scripted responses, and does
not measure model quality.

#### Scenario: Report states its scope

- **WHEN** a report is produced
- **THEN** it contains a statement that the results do not measure model quality
