package dev.diegap.refundguardrails.scripted;

/**
 * A deterministic script for one scenario. {@code followUp} answers the request
 * that carries the tool result, which is how a scripted tool call reaches the
 * assistant's final message without looping forever.
 */
public record ScriptedFixture(String name, ScriptedResponse initial, ScriptedResponse followUp) {

	public ScriptedFixture {
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("fixture name must not be blank");
		}
	}

	public static ScriptedFixture textOnly(String name, String content) {
		ScriptedResponse response = ScriptedResponse.text(content);
		return new ScriptedFixture(name, response, response);
	}

	public static ScriptedFixture withToolCall(String name, ScriptedResponse initial, String finalContent) {
		return new ScriptedFixture(name, initial, ScriptedResponse.text(finalContent));
	}

}
