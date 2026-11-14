package dev.diegap.refundguardrails.evals;

import java.util.List;

/** The case file and the scope statement every report repeats. */
public record EvalSuite(String suite, String scope, List<EvalCase> cases) {

	public EvalSuite {
		cases = cases == null ? List.of() : List.copyOf(cases);
	}

}
