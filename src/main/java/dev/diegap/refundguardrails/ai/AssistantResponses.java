package dev.diegap.refundguardrails.ai;

import java.util.List;

import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

/** Builds the short-circuit responses used when an input guard refuses a turn. */
final class AssistantResponses {

	private AssistantResponses() {
	}

	static ChatClientResponse text(String content) {
		ChatResponse response = new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
		return ChatClientResponse.builder().chatResponse(response).build();
	}

}
