package dev.diegap.refundguardrails.assistant;

import java.util.List;

import dev.diegap.refundguardrails.security.Redaction;

/**
 * The inspectable record of one turn. {@code modelBoundInput} is the redacted
 * user content the model would see; for a turn stopped by a guard it is still
 * populated so the interface can show what was withheld.
 */
public record TurnMetadata(String rawInput, String modelBoundInput, List<GuardEvaluation> guards,
		List<String> toolsInvoked, List<Redaction> redactions, FailureCategory failureCategory) {

	public TurnMetadata {
		guards = guards == null ? List.of() : List.copyOf(guards);
		toolsInvoked = toolsInvoked == null ? List.of() : List.copyOf(toolsInvoked);
		redactions = redactions == null ? List.of() : List.copyOf(redactions);
	}

}
