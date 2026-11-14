package dev.diegap.refundguardrails.scripted;

import java.util.List;

/**
 * A deterministic script for one scenario, expressed as a sequence of responses.
 * The number of tool messages already in the conversation selects the step, so a
 * turn can call several tools before the final text. Once the sequence is
 * exhausted, the last response repeats.
 */
public record ScriptedFixture(String name, List<ScriptedResponse> steps, ScriptedFailure failure) {

	public ScriptedFixture {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("fixture name must not be blank");
		}
		if (steps == null || steps.isEmpty()) {
			throw new IllegalArgumentException("a fixture needs at least one step");
		}
		steps = List.copyOf(steps);
	}

	public static ScriptedFixture textOnly(String name, String content) {
		return new ScriptedFixture(name, List.of(ScriptedResponse.text(content)), null);
	}

	public static ScriptedFixture sequence(String name, ScriptedResponse... steps) {
		return new ScriptedFixture(name, List.of(steps), null);
	}

	public static ScriptedFixture failure(String name, int status, String code, String message) {
		return new ScriptedFixture(name, List.of(ScriptedResponse.text("unreachable")),
				new ScriptedFailure(status, code, message));
	}

	public ScriptedResponse step(int toolMessageCount) {
		int index = Math.min(Math.max(toolMessageCount, 0), this.steps.size() - 1);
		return this.steps.get(index);
	}

}
