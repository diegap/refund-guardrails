package dev.diegap.refundguardrails.evals;

import java.util.List;

/** One declarative evaluation case. */
public record EvalCase(String name, String message, String scenario, EvalExpectations expect) {
}
