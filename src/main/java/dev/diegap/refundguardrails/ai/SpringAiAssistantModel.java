package dev.diegap.refundguardrails.ai;

import java.util.Map;

import dev.diegap.refundguardrails.assistant.TurnContext;
import dev.diegap.refundguardrails.assistant.port.out.AssistantModel;
import dev.diegap.refundguardrails.assistant.port.out.ModelFailure;
import dev.diegap.refundguardrails.assistant.port.out.ModelReply;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.stereotype.Component;

/**
 * Spring AI adapter for the assistant model. Owns the prompt invocation, the
 * advisor chain, the tool loop, and the framework exception classification.
 */
@Component
public class SpringAiAssistantModel implements AssistantModel {

	private final ChatClient chatClient;

	public SpringAiAssistantModel(ChatClient refundChatClient) {
		this.chatClient = refundChatClient;
	}

	@Override
	public ModelReply converse(TurnContext turn, String modelInput) {
		try {
			ChatClientResponse response = this.chatClient.prompt()
				.user(modelInput)
				.advisors((advisors) -> advisors.param(TurnContext.KEY, turn))
				.toolContext(Map.of(TurnContext.KEY, turn))
				.call()
				.chatClientResponse();
			return new ModelReply(textOf(response));
		}
		catch (RuntimeException ex) {
			throw new ModelFailure(causedByToolFailure(ex) ? ModelFailure.Kind.TOOL : ModelFailure.Kind.PROVIDER, ex);
		}
	}

	private static boolean causedByToolFailure(Throwable failure) {
		for (Throwable current = failure; current != null; current = current.getCause()) {
			if (current instanceof ToolExecutionException) {
				return true;
			}
			if (current instanceof IllegalStateException && current.getMessage() != null
					&& current.getMessage().toLowerCase(java.util.Locale.ROOT).contains("tool")) {
				return true;
			}
			if (current.getCause() == current) {
				break;
			}
		}
		return false;
	}

	private static String textOf(ChatClientResponse response) {
		if (response == null || response.chatResponse() == null || response.chatResponse().getResult() == null) {
			return "";
		}
		String text = response.chatResponse().getResult().getOutput().getText();
		return text == null ? "" : text;
	}

}
