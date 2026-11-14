# Refund Authorization Specification

## Purpose

Makes the decision to move money a deterministic server-side decision that the
model can request but cannot make, grant, widen, or forge.

## Requirements

### Requirement: Policy evaluation

Before any refund is authorized, the system SHALL evaluate the request against
policy: the amount must be positive, must not exceed the original charge, the
charge must belong to the referenced account and be in a refundable state, and
the amount must not exceed the configured auto-approval cap.

#### Scenario: Refund within policy

- **WHEN** a refund is requested for a refundable charge in an amount at or
  below the auto-approval cap
- **THEN** policy evaluation succeeds

#### Scenario: Refund above the cap

- **WHEN** the requested amount exceeds the auto-approval cap
- **THEN** policy evaluation fails and no authorization is issued

#### Scenario: Amount exceeds the original charge

- **WHEN** the requested amount is greater than the charge amount
- **THEN** policy evaluation fails and no authorization is issued

#### Scenario: Non-positive amount

- **WHEN** the requested amount is zero or negative
- **THEN** policy evaluation fails and no authorization is issued

### Requirement: Policy-minted single-use authorization

Only the policy component SHALL mint an authorization token, and each token SHALL
be bound to one charge, one amount and one session, SHALL expire after a short
configured lifetime, and SHALL be valid for a single use.

#### Scenario: Authorization is issued on success

- **WHEN** policy evaluation succeeds
- **THEN** an authorization token bound to that charge, amount and session is
  issued

#### Scenario: Authorization cannot be reused

- **WHEN** a successful refund has already consumed an authorization token
- **THEN** a second attempt with the same token is rejected

#### Scenario: Authorization expires

- **WHEN** a token is presented after its configured lifetime
- **THEN** the attempt is rejected

### Requirement: Tool-level enforcement

The refund tool SHALL execute only when presented with a valid authorization
token, and SHALL reject a missing, unknown, expired, already-used, or
mismatched token without changing any state.

#### Scenario: Valid token moves money

- **WHEN** the refund tool receives a valid token for the matching charge and
  amount
- **THEN** the refund is recorded and the token is consumed

#### Scenario: Missing or forged token

- **WHEN** the refund tool receives a missing, unknown, or forged token
- **THEN** the call is rejected and the refund ledger is unchanged

#### Scenario: Amount does not match the authorization

- **WHEN** the refund tool is called with an amount different from the one the
  token was bound to
- **THEN** the call is rejected and the token remains unused

### Requirement: Denial is explainable and escalates

A failed policy evaluation SHALL return a reason category that reveals no
sensitive data, SHALL NOT issue a token, and SHALL direct the assistant to offer
escalation to a human.

#### Scenario: Cap breach escalates

- **WHEN** a refund exceeds the cap
- **THEN** the response states that the request cannot be completed
  automatically, offers escalation, and contains no authorization token

### Requirement: No model-supplied authority

The system SHALL take policy limits, refund eligibility and authorization
decisions exclusively from server-side configuration and state, and SHALL ignore
any claim of approval, limit, or authority appearing in model output or user
input.

#### Scenario: Injection cannot widen authority

- **GIVEN** a conversation in which the model asserts that a refund was approved
  or that the limit does not apply
- **WHEN** the refund tool is called without a valid token
- **THEN** no refund occurs

### Requirement: Double refund prevention

A charge that has already been fully refunded SHALL NOT be refundable again.

#### Scenario: Second refund on the same charge

- **GIVEN** a charge that has been fully refunded
- **WHEN** another refund is requested for it
- **THEN** policy evaluation fails and no authorization is issued
