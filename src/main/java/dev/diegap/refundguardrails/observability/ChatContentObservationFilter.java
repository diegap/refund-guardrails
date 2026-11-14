package dev.diegap.refundguardrails.observability;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationFilter;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Spike 2.3: adds prompt and completion text as span attributes.
 * <p>
 * Spring AI 2.0's own content handlers only write to the application log, so a
 * filter is still required for {@code gen_ai.prompt} / {@code gen_ai.completion}
 * to reach the trace. Adapted from Langfuse's
 * {@code ChatModelCompletionContentObservationFilter}; Boot registers
 * {@link ObservationFilter} beans automatically.
 * <p>
 * Opt-in and redacted-only behaviour is added in task 8.3.
 */
@Component
@Profile("mock")
class ChatContentObservationFilter implements ObservationFilter {

	@Override
	public Observation.Context map(Observation.Context context) {
		if (!(context instanceof ChatModelObservationContext chatContext)) {
			return context;
		}

		chatContext.addHighCardinalityKeyValue(KeyValue.of("gen_ai.prompt", prompt(chatContext)));
		chatContext.addHighCardinalityKeyValue(KeyValue.of("gen_ai.completion", completion(chatContext)));
		return chatContext;
	}

	private String prompt(ChatModelObservationContext context) {
		return context.getRequest().getInstructions().stream()
				.map(content -> content.getText())
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
				.map(message -> message.getText())
				.filter(StringUtils::hasText)
				.collect(Collectors.joining("\n"));
	}

}
