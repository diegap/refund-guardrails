package dev.diegap.refundguardrails.assistant;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.stereotype.Component;

/**
 * The root span for one turn. Its session id and the guard/tool outcomes are
 * high-cardinality attributes; no prompt or completion content is added here.
 * The model-call span is created underneath it by Spring AI.
 */
@Component
public class TurnObservation {

	static final String NAME = "refund.turn";

	private final ObservationRegistry registry;

	public TurnObservation(ObservationRegistry registry) {
		this.registry = registry;
	}

	public Observation start(AssistantTurnRequest request) {
		return Observation.createNotStarted(NAME, this.registry)
			.highCardinalityKeyValue("session.id", request.sessionId())
			.highCardinalityKeyValue("turn.scenario",
					request.scenario() == null || request.scenario().isBlank() ? "none" : request.scenario());
	}

	public void annotate(Observation observation, TurnMetadata metadata) {
		for (GuardEvaluation guard : metadata.guards()) {
			observation.highCardinalityKeyValue("turn.guard." + guard.name() + ".outcome",
					guard.outcome().name());
			if (guard.reason() != null) {
				observation.highCardinalityKeyValue("turn.guard." + guard.name() + ".reason", guard.reason());
			}
		}
		observation.highCardinalityKeyValue("turn.tools", String.join(",", metadata.toolsInvoked()));
		observation.highCardinalityKeyValue("turn.redactions", Integer.toString(metadata.redactions().size()));
		observation.highCardinalityKeyValue("turn.failure", metadata.failureCategory().name());
	}

}
