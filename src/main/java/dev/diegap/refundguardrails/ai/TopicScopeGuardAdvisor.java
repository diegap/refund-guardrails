package dev.diegap.refundguardrails.ai;

import dev.diegap.refundguardrails.assistant.GuardOutcome;
import dev.diegap.refundguardrails.assistant.TopicScope;
import dev.diegap.refundguardrails.assistant.TurnContext;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/**
 * Third input guard: refuses text unrelated to accounts, charges, refunds, or
 * escalation before the model is called.
 */
@Component
public class TopicScopeGuardAdvisor implements CallAdvisor {

	public static final String REFUSAL = "I can only help with accounts, charges, refunds, or escalation to a human agent.";

	private final TopicScope scope;

	public TopicScopeGuardAdvisor(TopicScope scope) {
		this.scope = scope;
	}

	@Override
	public String getName() {
		return TurnContext.TOPIC_SCOPE;
	}

	@Override
	public int getOrder() {
		return AdvisorOrder.TOPIC_SCOPE;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		TurnContext turn = AdvisorContext.require(request);
		UserMessage userMessage = request.prompt().getUserMessage();
		String text = userMessage == null ? "" : userMessage.getText();

		if (!this.scope.isInScope(text)) {
			turn.guard(TurnContext.TOPIC_SCOPE, GuardOutcome.FAILED, "out_of_scope");
			turn.finalReply(REFUSAL);
			return AssistantResponses.text(REFUSAL);
		}

		turn.guard(TurnContext.TOPIC_SCOPE, GuardOutcome.PASSED);
		return chain.nextCall(request);
	}

}
