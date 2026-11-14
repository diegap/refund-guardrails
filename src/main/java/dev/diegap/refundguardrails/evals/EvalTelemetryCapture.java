package dev.diegap.refundguardrails.evals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Captures the observation contexts a run emits so the evaluation suite can
 * apply the privacy invariant to telemetry as well as to prompts. Only present
 * under the {@code eval} profile.
 */
@Component
@Profile("eval")
public class EvalTelemetryCapture implements ObservationHandler<Observation.Context> {

	private final List<Observation.Context> contexts = new CopyOnWriteArrayList<>();

	@Override
	public void onStop(Observation.Context context) {
		this.contexts.add(context);
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return true;
	}

	public List<Observation.Context> contexts() {
		return List.copyOf(this.contexts);
	}

	public void clear() {
		this.contexts.clear();
	}

}
