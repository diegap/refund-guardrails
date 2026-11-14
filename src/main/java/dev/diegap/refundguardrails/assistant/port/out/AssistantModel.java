package dev.diegap.refundguardrails.assistant.port.out;

import dev.diegap.refundguardrails.assistant.TurnContext;

/**
 * Driven port for the language model. The adapter owns the prompt, the advisor
 * chain, and the tool loop; the application only asks for the final text or a
 * typed failure.
 */
public interface AssistantModel {

	ModelReply converse(TurnContext turn, String modelInput);

}
