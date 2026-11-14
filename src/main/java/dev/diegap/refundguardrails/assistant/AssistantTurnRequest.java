package dev.diegap.refundguardrails.assistant;

/** What the caller sends for one turn. */
public record AssistantTurnRequest(String sessionId, String message, String scenario) {

	public AssistantTurnRequest {
		sessionId = sessionId == null || sessionId.isBlank() ? "default" : sessionId;
		message = message == null ? "" : message;
	}

}
