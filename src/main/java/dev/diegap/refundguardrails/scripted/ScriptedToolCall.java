package dev.diegap.refundguardrails.scripted;

/**
 * A tool call the scripted provider asks the model client to perform.
 * {@code arguments} is a JSON document, serialized as a string exactly as the
 * OpenAI API does.
 */
public record ScriptedToolCall(String id, String name, String arguments) {
}
