package dev.diegap.refundguardrails.assistant;

/** One guard's decision for this turn, safe to show to a user. */
public record GuardEvaluation(String name, GuardOutcome outcome) {
}
