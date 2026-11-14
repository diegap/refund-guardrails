package dev.diegap.refundguardrails.assistant;

/** The result of creating a human escalation. */
public record EscalationResult(String ticketId, String status, String message) {
}
