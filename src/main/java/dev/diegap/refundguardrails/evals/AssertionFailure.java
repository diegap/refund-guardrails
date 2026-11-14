package dev.diegap.refundguardrails.evals;

/** A failed assertion, named by type with a detail that never includes a PAN. */
public record AssertionFailure(String assertion, String detail) {
}
