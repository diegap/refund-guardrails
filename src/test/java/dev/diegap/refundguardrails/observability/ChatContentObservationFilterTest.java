package dev.diegap.refundguardrails.observability;

import java.util.List;

import dev.diegap.refundguardrails.security.OutputRedactor;
import dev.diegap.refundguardrails.security.PanDetector;
import io.micrometer.common.KeyValue;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.Prompt;

import static org.assertj.core.api.Assertions.assertThat;

class ChatContentObservationFilterTest {

	private static final String VISA_FORMATTED = "4242 4242 4242 4242";

	private static final String MASTERCARD_FORMATTED = "5555 5555 5555 4444";

	private final OutputRedactor redactor = new OutputRedactor(new PanDetector());

	@Test
	void contentAttributesAreAbsentWhenDisabled() {
		ChatContentObservationFilter filter = new ChatContentObservationFilter(
				new TelemetryProperties(new TelemetryProperties.Content(false)), this.redactor);
		ChatModelObservationContext context = context();

		filter.map(context);

		assertThat(context.getHighCardinalityKeyValue(ChatContentObservationFilter.PROMPT)).isNull();
		assertThat(context.getHighCardinalityKeyValue(ChatContentObservationFilter.COMPLETION)).isNull();
	}

	@Test
	void enabledContentIsRedactedAndKeepsTheToken() {
		ChatContentObservationFilter filter = new ChatContentObservationFilter(
				new TelemetryProperties(new TelemetryProperties.Content(true)), this.redactor);
		ChatModelObservationContext context = context();

		filter.map(context);

		KeyValue prompt = context.getHighCardinalityKeyValue(ChatContentObservationFilter.PROMPT);
		assertThat(prompt).isNotNull();
		assertThat(prompt.getValue()).contains("[[CARD_1]]").contains(OutputRedactor.CARD_MARKER)
			.doesNotContain(VISA_FORMATTED);

		KeyValue completion = context.getHighCardinalityKeyValue(ChatContentObservationFilter.COMPLETION);
		assertThat(completion).isNotNull();
		assertThat(completion.getValue()).contains(OutputRedactor.CARD_MARKER, OutputRedactor.EMAIL_MARKER)
			.doesNotContain(MASTERCARD_FORMATTED)
			.doesNotContain("ada@example.com");
	}

	private ChatModelObservationContext context() {
		Prompt prompt = new Prompt(List.of(new SystemMessage("You are a refund assistant."),
				new UserMessage("Refund the duplicate on token [[CARD_1]] (raw card " + VISA_FORMATTED + ").")));
		ChatModelObservationContext context = ChatModelObservationContext.builder()
			.prompt(prompt)
			.provider("openai")
			.streaming(false)
			.build();
		context.setResponse(new ChatResponse(List.of(new Generation(
				new AssistantMessage("The card " + MASTERCARD_FORMATTED + " failed; contact ada@example.com.")))));
		return context;
	}

}
