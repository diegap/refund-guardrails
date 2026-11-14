package dev.diegap.refundguardrails.ai;

import java.util.List;

import dev.diegap.refundguardrails.assistant.GuardOutcome;
import dev.diegap.refundguardrails.assistant.TurnContext;
import dev.diegap.refundguardrails.security.OutputRedactor;
import dev.diegap.refundguardrails.security.RedactedText;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Component;

/**
 * Last advisor: scans the final assistant text before delivery and records the
 * output redactions. Input redaction should make this a no-op, but a
 * prompt-injected or misbehaving model must not be able to echo a card number.
 */
@Component
public class OutputScanningAdvisor implements CallAdvisor {

	private final OutputRedactor redactor;

	public OutputScanningAdvisor(OutputRedactor redactor) {
		this.redactor = redactor;
	}

	@Override
	public String getName() {
		return TurnContext.OUTPUT_SCANNING;
	}

	@Override
	public int getOrder() {
		return AdvisorOrder.OUTPUT_SCANNING;
	}

	@Override
	public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
		TurnContext turn = AdvisorContext.require(request);
		ChatClientResponse response = chain.nextCall(request);

		String text = textOf(response);
		RedactedText scanned = this.redactor.redact(text);
		turn.addRedactions(scanned.redactions());
		turn.finalReply(scanned.text());
		turn.guard(TurnContext.OUTPUT_SCANNING, GuardOutcome.PASSED);

		return withText(response, scanned.text());
	}

	private static String textOf(ChatClientResponse response) {
		if (response == null || response.chatResponse() == null || response.chatResponse().getResult() == null) {
			return "";
		}
		AssistantMessage output = response.chatResponse().getResult().getOutput();
		return output == null || output.getText() == null ? "" : output.getText();
	}

	private static ChatClientResponse withText(ChatClientResponse response, String text) {
		if (response == null || response.chatResponse() == null || response.chatResponse().getResult() == null) {
			return response;
		}
		AssistantMessage original = response.chatResponse().getResult().getOutput();
		AssistantMessage redacted = original.mutate().content(text).build();
		Generation generation = new Generation(redacted, response.chatResponse().getResult().getMetadata());
		ChatResponse chatResponse = ChatResponse.builder()
			.from(response.chatResponse())
			.generations(List.of(generation))
			.build();
		return response.mutate().chatResponse(chatResponse).build();
	}

}
