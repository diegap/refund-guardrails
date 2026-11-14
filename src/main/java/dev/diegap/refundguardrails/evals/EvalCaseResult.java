package dev.diegap.refundguardrails.evals;

import java.util.List;

/** The outcome of one case, including every failed assertion. */
public record EvalCaseResult(String name, String outcome, List<AssertionFailure> failedAssertions) {

	public EvalCaseResult {
		failedAssertions = failedAssertions == null ? List.of() : List.copyOf(failedAssertions);
	}

	public boolean passed() {
		return "PASSED".equals(this.outcome);
	}

}
