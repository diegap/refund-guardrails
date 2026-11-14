package dev.diegap.refundguardrails.observability;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import dev.diegap.refundguardrails.security.OutputRedactor;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Adds prompt and completion text as span attributes, opt-in and redacted-only.
 * <p>
 * Off by default: no content attributes are written. When
 * {@code app.telemetry.content.enabled=true}, both strings pass through the
 * output redactor, so a card number that a model echoes becomes a marker rather
 * than a span attribute. Spring AI's own content handlers only write to the
 * application log, so a filter is still required for the trace.
 */
@Component
class ChatContentObservationFilter implements ObservationFilter {

	static final String PROMPT = "gen_ai.prompt";

	static final String COMPLETION = "gen_ai.completion";

	private final TelemetryProperties properties;

	private final OutputRedactor redactor;

	ChatContentObservationFilter(TelemetryProperties properties, OutputRedactor redactor) {
		this.properties = properties;
		this.redactor = redactor;
	}

	@Override
	public Observation.Context map(Observation.Context context) {
		if (!this.properties.content().enabled()) {
			return context;
		}
		if (!(context instanceof ChatModelObservationContext chatContext)) {
			return context;
		}

		chatContext.addHighCardinalityKeyValue(KeyValue.of(PROMPT, redact(prompt(chatContext))));
		chatContext.addHighCardinalityKeyValue(KeyValue.of(COMPLETION, redact(completion(chatContext))));
		return chatContext;
	}

	private String redact(String text) {
		return this.redactor.redact(text).text();
	}

	private String prompt(ChatModelObservationContext context) {
		return context.getRequest().getInstructions().stream()
			.map((content) -> content.getText())
			.filter(StringUtils::hasText)
			.collect(Collectors.joining("\n"));
	}

	private String completion(ChatModelObservationContext context) {
		List<Generation> results = Optional.ofNullable(context.getResponse())
			.map(ChatResponse::getResults)
			.orElse(List.of());
		return results.stream()
			.map(Generation::getOutput)
			.filter(Objects::nonNull)
			.map((message) -> message.getText())
			.filter(StringUtils::hasText)
			.collect(Collectors.joining("\n"));
	}

}
