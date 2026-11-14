package dev.diegap.refundguardrails.assistant;

/**
 * A technical failure category. {@link #NONE} means the turn completed, even if a
 * guard or the refund policy refused the request.
 */
public enum FailureCategory {

	NONE, PROVIDER_ERROR, TOOL_ERROR

}
