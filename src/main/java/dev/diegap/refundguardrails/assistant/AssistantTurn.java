package dev.diegap.refundguardrails.assistant;

/** One assistant answer with the metadata the interface needs to explain it. */
public record AssistantTurn(String sessionId, String reply, TurnMetadata metadata) {
}
