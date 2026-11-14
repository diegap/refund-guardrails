package dev.diegap.refundguardrails.ai;

import java.util.List;
import java.util.Map;

import dev.diegap.refundguardrails.assistant.TurnContext;
import dev.diegap.refundguardrails.security.InputRedactor;
import dev.diegap.refundguardrails.security.PanDetector;
import dev.diegap.refundguardrails.security.PanVault;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

class AdvisorOrderTest {

	private static final String PAN = "4242 4242 4242 4242";

	@Test
	void advisorsRunInTheDeclaredOrder() {
		assertThat(AdvisorOrder.CARD_REDACTION).isLessThan(AdvisorOrder.PROMPT_INJECTION);
		assertThat(AdvisorOrder.PROMPT_INJECTION).isLessThan(AdvisorOrder.TOPIC_SCOPE);
		assertThat(AdvisorOrder.TOPIC_SCOPE).isLessThan(AdvisorOrder.OUTPUT_SCANNING);
		assertThat(AdvisorOrder.OUTPUT_SCANNING).isLessThan(1000);
	}

	@Test
	void redactionHappensBeforeAnyLaterAdvisorCanObserveThePrompt() {
		TurnContext turn = new TurnContext("order-test", "Please refund my card " + PAN);
		ChatClientRequest request = ChatClientRequest.builder()
			.prompt(new Prompt(List.of(new SystemMessage("system"), new UserMessage(turn.rawInput()))))
			.context(Map.of(TurnContext.KEY, turn))
			.build();

		CardRedactionAdvisor redaction = new CardRedactionAdvisor(
				new InputRedactor(new PanDetector(), new PanVault()));
		RecordingChain chain = new RecordingChain();

		redaction.adviseCall(request, chain);

		assertThat(chain.observed.prompt().getUserMessage().getText()).isEqualTo("Please refund my card [[CARD_1]]");
		assertThat(chain.observed.prompt().getUserMessage().getText()).doesNotContain("4242 4242 4242 4242");
		assertThat(turn.metadata().modelBoundInput()).isEqualTo("Please refund my card [[CARD_1]]");
		assertThat(turn.metadata().redactions()).singleElement().satisfies((redactionRecord) -> {
			assertThat(redactionRecord.token()).isEqualTo("[[CARD_1]]");
			assertThat(redactionRecord.lastFour()).isEqualTo("4242");
		});
	}

	private static final class RecordingChain implements CallAdvisorChain {

		private ChatClientRequest observed;

		@Override
		public ChatClientResponse nextCall(ChatClientRequest request) {
			this.observed = request;
			return AssistantResponses.text("done");
		}

		@Override
		public List<CallAdvisor> getCallAdvisors() {
			return List.of();
		}

		@Override
		public CallAdvisorChain copy(CallAdvisor advisor) {
			return this;
		}

	}

}
