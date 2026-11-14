package dev.diegap.refundguardrails.ai;

import dev.diegap.refundguardrails.assistant.TurnContext;
import org.springframework.ai.chat.client.ChatClientRequest;

/** Extracts the per-turn context from a Spring AI request. */
final class AdvisorContext {

	private AdvisorContext() {
	}

	static TurnContext require(ChatClientRequest request) {
		Object value = request.context().get(TurnContext.KEY);
		if (value instanceof TurnContext context) {
			return context;
		}
		throw new IllegalStateException("advisors require a turn context parameter");
	}

}
