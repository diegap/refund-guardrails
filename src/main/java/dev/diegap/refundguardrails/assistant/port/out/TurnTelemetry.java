package dev.diegap.refundguardrails.assistant.port.out;

import dev.diegap.refundguardrails.assistant.AssistantTurnRequest;
import dev.diegap.refundguardrails.assistant.TurnMetadata;

/**
 * Port for the root turn span. The assistant service depends on this interface
 * instead of a Micrometer type, so the span implementation lives outside the
 * use case.
 */
public interface TurnTelemetry {

	TurnScope start(AssistantTurnRequest request);

	interface TurnScope extends AutoCloseable {

		void annotate(TurnMetadata metadata);

		@Override
		void close();

	}

}
