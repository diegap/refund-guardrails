# Spec Delta

## Purpose

Keeps card numbers out of the model's context, out of tool inputs, and out of
telemetry, while still allowing the assistant to identify a card to the last four
digits.

## ADDED Requirements

### Requirement: Card number detection

The system SHALL detect a card number in user input using length, issuer prefix,
and a Luhn check, treating spaces and dashes as separators, and SHALL NOT
classify a number that fails those checks as a card number.

#### Scenario: Formatted card number is detected

- **WHEN** the input contains a Luhn-valid number with a known issuer prefix,
  written with spaces or dashes
- **THEN** the number is classified as a card number and its span boundaries are
  recorded

#### Scenario: Order number is left alone

- **WHEN** the input contains a numeric identifier that fails the Luhn check or
  has no plausible issuer prefix
- **THEN** the identifier is passed through unchanged

### Requirement: Vault substitution before the model call

Before any content is sent to the model, the system SHALL replace each detected
card number with a session-scoped token, SHALL reuse the same token for the same
card number within a session, and SHALL keep the mapping only in memory.

#### Scenario: Card number becomes a token

- **WHEN** a user message contains a card number
- **THEN** the model-bound message contains a token such as `[[CARD_1]]` in its
  place and the raw number is absent

#### Scenario: Same card, same token

- **WHEN** the same card number appears in two turns of one session
- **THEN** both turns use the same token

#### Scenario: Different sessions do not share tokens

- **GIVEN** two sessions each containing a card number
- **WHEN** either session is inspected
- **THEN** its tokens resolve only within that session

### Requirement: Last-four projection

Tools that identify a card SHALL return only the last four digits and SHALL NOT
return the full number, the token's raw value, or any other cardholder data.

#### Scenario: Card lookup returns only last four

- **WHEN** a tool resolves a card token
- **THEN** the tool result contains the last four digits and no more than four
  digits of the card number

### Requirement: No card number reaches the model or telemetry

Model-bound messages, tool results, span attributes, span events, and log lines
SHALL NOT contain a full card number in any turn, including turns where the user
supplied one.

#### Scenario: Model-bound request is clean

- **WHEN** a turn that contained a card number completes
- **THEN** the captured model-bound request matches no card number pattern

#### Scenario: Telemetry is clean

- **WHEN** that turn's trace data is inspected
- **THEN** no span attribute, span event, or log line contains a full card
  number

### Requirement: Output scanning

The system SHALL scan model output for card numbers and email addresses before
delivery and SHALL redact any match.

#### Scenario: Model echoes a card number

- **WHEN** model output contains a card number pattern
- **THEN** the delivered response replaces it with a redaction marker

#### Scenario: Clean output is unchanged

- **WHEN** model output contains no sensitive pattern
- **THEN** the delivered response is byte-identical to the model output

### Requirement: Vault lifetime

The vault SHALL be scoped to a session, SHALL NOT be written to disk, and SHALL
be discarded when the session ends or the application restarts.

#### Scenario: Restart forgets the vault

- **GIVEN** a session whose card number was vaulted
- **WHEN** the application restarts
- **THEN** the token no longer resolves

### Requirement: Redaction is observable

The system SHALL report, per turn, which redactions occurred and at which stage,
so the interface can show the user's raw input beside the redacted model-bound
content.

#### Scenario: Redaction report accompanies the turn

- **WHEN** a turn contains a card number
- **THEN** the turn result identifies the redaction stage, the token used, and
  the last four digits that remain visible
