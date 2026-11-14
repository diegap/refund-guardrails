package dev.diegap.refundguardrails.evals;

import java.util.List;

/**
 * Assertions declared by a case. A {@code null} field means the case does not
 * assert that property. {@code noPanInModelBound} is only ever true; the privacy
 * invariant runs regardless.
 */
public record EvalExpectations(List<String> guardsFired, List<String> guardsPassed, Boolean refusal,
		List<String> toolSequence, List<String> contains, List<String> omits, Boolean noPanInModelBound,
		List<String> modelBoundContains, Integer ledgerRefunds, Boolean ledgerUnchanged,
		String failureCategory) {
}
