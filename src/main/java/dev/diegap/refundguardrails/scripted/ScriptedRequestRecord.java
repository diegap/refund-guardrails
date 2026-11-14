package dev.diegap.refundguardrails.scripted;

/**
 * One captured model-bound request. {@code rawBody} is the exact JSON the
 * provider received; it stays in memory only.
 */
public record ScriptedRequestRecord(String sessionId, String requestedScenario, String fixtureName,
		boolean fallback, int toolMessageCount, String rawBody, java.time.Instant receivedAt) {

	public boolean afterToolCall() {
		return this.toolMessageCount > 0;
	}

}
