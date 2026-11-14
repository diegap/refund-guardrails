package dev.diegap.refundguardrails.ai;

import java.util.ArrayList;
import java.util.List;

import dev.diegap.refundguardrails.assistant.GuardOutcome;
import dev.diegap.refundguardrails.assistant.TurnContext;
import dev.diegap.refundguardrails.security.InputRedactor;
import dev.diegap.refundguardrails.security.RedactedText;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

/**
 * First input guard: replaces card numbers with session tokens before any later
 * advisor, the tool loop, or the model can observe the prompt.
 */
@Component
public class CardRedactionAdvisor implements CallAdvisor {

	private final InputRedactor redactor;

	public CardRedactionAdvisor(InputRedactor redactor) {
		this.redactor = redactor;
	}

	@Override
	public String getName() {
		return TurnContext.CARD_REDACTION;
	}

	@Override
	public int getOrder() {
		return AdvisorOrder.CARD_REDACTION;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		TurnContext turn = AdvisorContext.require(request);
		Prompt prompt = request.prompt();
		List<Message> messages = new ArrayList<>(prompt.getInstructions().size());

		for (Message message : prompt.getInstructions()) {
			if (message instanceof UserMessage userMessage) {
				RedactedText redacted = this.redactor.redact(turn.sessionId(), userMessage.getText());
				messages.add(userMessage.mutate().text(redacted.text()).build());
				turn.modelBoundInput(redacted.text());
				turn.addRedactions(redacted.redactions());
			}
			else {
				messages.add(message);
			}
		}

		turn.guard(TurnContext.CARD_REDACTION, GuardOutcome.PASSED);
		Prompt redactedPrompt = new Prompt(messages, prompt.getOptions());
		return chain.nextCall(request.mutate().prompt(redactedPrompt).build());
	}

}
