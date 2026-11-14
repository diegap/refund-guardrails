package dev.diegap.refundguardrails.assistant;

import java.util.Optional;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

/**
 * Second input guard: refuses overt instruction-override attempts and never
 * calls the model. This guard is intentionally imperfect; policy remains the
 * authority on money movement.
 */
@Component
public class PromptInjectionGuardAdvisor implements CallAdvisor {

	public static final String REFUSAL = "I can't follow instructions that try to change my rules or grant exceptions. "
			+ "I can help with account, charge, or refund questions, or escalate to a human agent.";

	private final InjectionHeuristic heuristic;

	public PromptInjectionGuardAdvisor(InjectionHeuristic heuristic) {
		this.heuristic = heuristic;
	}

	@Override
	public String getName() {
		return TurnContext.PROMPT_INJECTION;
	}

	@Override
	public int getOrder() {
		return AdvisorOrder.PROMPT_INJECTION;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		TurnContext turn = TurnContext.require(request);
		UserMessage userMessage = request.prompt().getUserMessage();
		String text = userMessage == null ? "" : userMessage.getText();

		Optional<String> reason = this.heuristic.detect(text);
		if (reason.isPresent()) {
			turn.guard(TurnContext.PROMPT_INJECTION, GuardOutcome.FAILED, reason.get());
			turn.finalReply(REFUSAL);
			return AssistantResponses.text(REFUSAL);
		}

		turn.guard(TurnContext.PROMPT_INJECTION, GuardOutcome.PASSED);
		return chain.nextCall(request);
	}

}
