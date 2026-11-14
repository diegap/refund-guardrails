package dev.diegap.refundguardrails.evals;

import java.util.List;

import dev.diegap.refundguardrails.assistant.AssistantTurn;
import dev.diegap.refundguardrails.scripted.ScriptedRequestRecord;
import io.micrometer.observation.Observation;

/** Everything an assertion may inspect about one case. */
public record EvalObservation(AssistantTurn turn, List<ScriptedRequestRecord> requests,
		List<Observation.Context> telemetry, int refundCount) {

	public EvalObservation {
		requests = requests == null ? List.of() : List.copyOf(requests);
		telemetry = telemetry == null ? List.of() : List.copyOf(telemetry);
	}

}
