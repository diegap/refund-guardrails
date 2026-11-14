package dev.diegap.refundguardrails.observability;

import dev.diegap.refundguardrails.assistant.AssistantTurnRequest;
import dev.diegap.refundguardrails.assistant.GuardEvaluation;
import dev.diegap.refundguardrails.assistant.TurnMetadata;
import dev.diegap.refundguardrails.assistant.port.out.TurnTelemetry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;

/**
 * Micrometer implementation of the turn telemetry port. The root span is opened
 * for the whole turn so the advisor, tool, and model spans nest beneath it. Its
 * session id and guard/tool outcomes are high-cardinality attributes; no prompt
 * or completion content is added here.
 */
@Component
public class MicrometerTurnTelemetry implements TurnTelemetry {

	static final String NAME = "refund.turn";

	private final ObservationRegistry registry;

	public MicrometerTurnTelemetry(ObservationRegistry registry) {
		this.registry = registry;
	}

	@Override
	public TurnScope start(AssistantTurnRequest request) {
		Observation observation = Observation.createNotStarted(NAME, this.registry)
			.highCardinalityKeyValue("session.id", request.sessionId())
			.highCardinalityKeyValue("turn.scenario",
					request.scenario() == null || request.scenario().isBlank() ? "none" : request.scenario());
		return new Scope(observation);
	}

	private static final class Scope implements TurnScope {

		private final Observation observation;

		private final Observation.Scope scope;

		private Scope(Observation observation) {
			this.observation = observation;
			this.observation.start();
			this.scope = observation.openScope();
		}

		@Override
		public void annotate(TurnMetadata metadata) {
			for (GuardEvaluation guard : metadata.guards()) {
				this.observation.highCardinalityKeyValue("turn.guard." + guard.name() + ".outcome",
						guard.outcome().name());
				if (guard.reason() != null) {
					this.observation.highCardinalityKeyValue("turn.guard." + guard.name() + ".reason", guard.reason());
				}
			}
			this.observation.highCardinalityKeyValue("turn.tools", String.join(",", metadata.toolsInvoked()));
			this.observation.highCardinalityKeyValue("turn.redactions",
					Integer.toString(metadata.redactions().size()));
			this.observation.highCardinalityKeyValue("turn.failure", metadata.failureCategory().name());
		}

		@Override
		public void close() {
			this.scope.close();
			this.observation.stop();
		}

	}

}
