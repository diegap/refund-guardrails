package dev.diegap.refundguardrails.evals;

import java.util.List;

/** The whole run: suite metadata, totals, and per-case outcomes. */
public record EvalReport(String suite, String scope, int total, int passed, int failed,
		List<EvalCaseResult> cases) {

	public EvalReport {
		cases = cases == null ? List.of() : List.copyOf(cases);
	}

}
