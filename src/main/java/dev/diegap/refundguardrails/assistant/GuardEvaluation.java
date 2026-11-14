package dev.diegap.refundguardrails.assistant;

/** One guard's decision for this turn, safe to show to a user. */
public record GuardEvaluation(String name, GuardOutcome outcome, String reason) {

	public static GuardEvaluation passed(String name) {
		return new GuardEvaluation(name, GuardOutcome.PASSED, null);
	}

	public static GuardEvaluation failed(String name, String reason) {
		return new GuardEvaluation(name, GuardOutcome.FAILED, reason);
	}

}
