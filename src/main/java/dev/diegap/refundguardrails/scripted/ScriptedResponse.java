package dev.diegap.refundguardrails.scripted;

import java.util.List;

/** One assistant answer: either text, tool calls, or both. */
public record ScriptedResponse(String content, List<ScriptedToolCall> toolCalls) {

	public ScriptedResponse {
		toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
	}

	public static ScriptedResponse text(String content) {
		return new ScriptedResponse(content, List.of());
	}

	public static ScriptedResponse toolCall(String id, String name, String arguments) {
		return new ScriptedResponse(null, List.of(new ScriptedToolCall(id, name, arguments)));
	}

	public boolean hasToolCalls() {
		return !this.toolCalls.isEmpty();
	}

}
