package dev.diegap.refundguardrails.scripted;

/**
 * A deterministic provider failure. The controller returns this status instead of
 * a completion, which lets the assistant's safe-failure path be tested through
 * the real HTTP client.
 */
public record ScriptedFailure(int status, String code, String message) {
}
