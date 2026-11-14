# Spec Delta

## Purpose

Provides deterministic, OpenAI-compatible chat completions from inside the
application so the agent can run end to end without a network, an API key, or a
cloud account, while still exercising the real provider client path.

## ADDED Requirements

### Requirement: OpenAI-compatible chat completions endpoint

The system SHALL expose a local chat-completions endpoint that accepts
OpenAI-style requests carrying `messages`, `tools`, and `model`, and returns a
non-streaming response containing `choices`, a `message`, `tool_calls` when
scripted, and `usage` counts.

#### Scenario: Well-formed request receives a well-formed response

- **GIVEN** the scripted provider is active
- **WHEN** a client posts a chat-completions request with a user message
- **THEN** the response is a valid chat-completions payload with one choice, a
  message content or tool calls, and non-negative token usage

#### Scenario: Streaming is rejected explicitly

- **WHEN** a request sets `stream` to true
- **THEN** the provider returns a documented error indicating streaming is
  unsupported, rather than a partial or malformed stream

#### Scenario: Endpoint is absent outside mock mode

- **GIVEN** the application runs without the mock profile
- **WHEN** the chat-completions path is requested
- **THEN** no scripted response is served

### Requirement: Deterministic fixture routing

The provider SHALL select a scripted response from an explicit scenario marker
in the request, SHALL fall back to a default fixture when no marker matches, and
SHALL return identical content for identical requests within a run.

#### Scenario: Repeated requests are identical

- **WHEN** the same request is sent twice
- **THEN** both responses contain identical message content and tool calls

#### Scenario: Unknown scenario falls back

- **WHEN** a request carries a marker that matches no fixture
- **THEN** the default fixture is returned and the fallback is recorded for
  inspection

#### Scenario: Concurrent sessions do not interfere

- **GIVEN** two sessions active at the same time
- **WHEN** each sends a request with a different scenario marker
- **THEN** each receives the response for its own scenario

### Requirement: Scripted tool calls

A fixture MAY return tool calls, and any returned tool call SHALL carry a tool
name and JSON arguments valid against that tool's declared schema so that the
assistant tool loop can complete.

#### Scenario: Scripted tool call drives the tool loop

- **WHEN** the selected fixture returns a tool call for an allowed tool
- **THEN** the assistant invokes that tool with the parsed arguments and
  produces a follow-up model request

#### Scenario: Malformed fixture fails loudly

- **WHEN** a fixture returns tool call arguments that are not valid JSON
- **THEN** the request fails with a clear error identifying the fixture, rather
  than silently degrading

### Requirement: Provider selection by configuration

The model provider's base URL, credentials, and model name SHALL be supplied
through configuration or environment variables, so that switching to a real
provider requires no code change.

#### Scenario: Pointing at another provider needs no code change

- **GIVEN** a different base URL, key, and model name in configuration
- **WHEN** the application starts
- **THEN** model requests are sent to that endpoint through the same code path

#### Scenario: Missing credentials are a configuration error

- **GIVEN** mock mode is disabled and no credentials are configured
- **WHEN** the application starts
- **THEN** startup fails with a message naming the missing configuration

### Requirement: Model-bound request capture

The provider SHALL record each received request body in memory, keyed by
session, for display in the interface and use by the evaluation suite, and SHALL
NOT persist it to disk or write it to application logs.

#### Scenario: Captured request is available for inspection

- **WHEN** a chat turn completes
- **THEN** the exact model-bound request for that session is retrievable and
  contains the redacted content the model received

#### Scenario: Capture is memory-only

- **WHEN** the application shuts down or a session is discarded
- **THEN** no captured request body remains on disk or in logs

### Requirement: Offline operation

The application SHALL start and serve chat turns with no API key, no outbound
model traffic, and no cloud dependency while the scripted provider is active.

#### Scenario: Cold start with nothing configured

- **GIVEN** a clean checkout, no environment variables, and no network access
- **WHEN** the application starts and a chat turn is submitted
- **THEN** the turn completes with a scripted response
