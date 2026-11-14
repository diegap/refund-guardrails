package dev.diegap.refundguardrails.scripted;

import java.time.Instant;

/**
 * One captured model-bound request. {@code rawBody} is the exact JSON the
 * provider received; it stays in memory only.
 */
public record ScriptedRequestRecord(String sessionId, String requestedScenario, String fixtureName,
		boolean fallback, boolean afterToolCall, String rawBody, Instant receivedAt) {
}
