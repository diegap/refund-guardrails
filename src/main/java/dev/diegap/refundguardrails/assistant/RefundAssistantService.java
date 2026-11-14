package dev.diegap.refundguardrails.assistant;

import dev.diegap.refundguardrails.assistant.port.in.RefundAssistant;
import dev.diegap.refundguardrails.assistant.port.out.AssistantModel;
import dev.diegap.refundguardrails.assistant.port.out.ModelFailure;
import dev.diegap.refundguardrails.assistant.port.out.ModelReply;
import dev.diegap.refundguardrails.assistant.port.out.TurnTelemetry;
import dev.diegap.refundguardrails.security.OutputRedactor;
import dev.diegap.refundguardrails.security.RedactedText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * One conversational turn. Input guards run inside the advisor chain; this
 * service supplies the per-turn context, turns failures into safe answers, and
 * returns the metadata the interface needs.
 */
@Service
public class RefundAssistantService implements RefundAssistant {

	private static final Logger log = LoggerFactory.getLogger(RefundAssistantService.class);

	private final AssistantModel model;

	private final OutputRedactor outputRedactor;

	private final TurnTelemetry telemetry;

	public RefundAssistantService(AssistantModel model, OutputRedactor outputRedactor,
			TurnTelemetry telemetry) {
		this.model = model;
		this.outputRedactor = outputRedactor;
		this.telemetry = telemetry;
	}

	@Override
	public AssistantTurn chat(AssistantTurnRequest request) {
		TurnContext turn = new TurnContext(request.sessionId(), request.message());
		TurnTelemetry.TurnScope telemetry = this.telemetry.start(request);
		try {
			return doChat(request, turn);
		}
		finally {
			telemetry.annotate(turn.metadata());
			telemetry.close();
		}
	}

	private AssistantTurn doChat(AssistantTurnRequest request, TurnContext turn) {
		try {
			ModelReply response = this.model.converse(turn, modelInput(request));

			String reply = turn.finalReply();
			if (reply == null) {
				reply = scan(turn, response.text());
			}
			return new AssistantTurn(turn.sessionId(), reply, turn.metadata());
		}
		catch (ModelFailure ex) {
			if (ex.kind() == ModelFailure.Kind.TOOL) {
				log.warn("tool failure in session {}: {}", turn.sessionId(), ex.getClass().getSimpleName());
				return safeFailure(turn, FailureCategory.TOOL_ERROR, AssistantReplies.SAFE_TOOL_FAILURE);
			}
			log.warn("provider failure in session {}: {}", turn.sessionId(), ex.getClass().getSimpleName());
			return safeFailure(turn, FailureCategory.PROVIDER_ERROR, AssistantReplies.SAFE_PROVIDER_FAILURE);
		}
	}

	private static String modelInput(AssistantTurnRequest request) {
		StringBuilder input = new StringBuilder(request.message());
		input.append("\n[[session:").append(markerSafe(request.sessionId())).append("]]");
		if (request.scenario() != null && !request.scenario().isBlank()) {
			input.append("[[scenario:").append(markerSafe(request.scenario())).append("]]");
		}
		return input.toString();
	}

	private static String markerSafe(String value) {
		return value.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	private String scan(TurnContext turn, String text) {
		RedactedText scanned = this.outputRedactor.redact(text);
		turn.addRedactions(scanned.redactions());
		turn.finalReply(scanned.text());
		if (!turn.hasGuard(TurnContext.OUTPUT_SCANNING)) {
			turn.guard(TurnContext.OUTPUT_SCANNING, GuardOutcome.PASSED);
		}
		return scanned.text();
	}

	private AssistantTurn safeFailure(TurnContext turn, FailureCategory category, String safeReply) {
		turn.fail(category);
		if (!turn.hasGuard(TurnContext.OUTPUT_SCANNING)) {
			RedactedText scanned = this.outputRedactor.redact(safeReply);
			turn.addRedactions(scanned.redactions());
		}
		turn.finalReply(safeReply);
		if (!turn.hasGuard(TurnContext.OUTPUT_SCANNING)) {
			turn.guard(TurnContext.OUTPUT_SCANNING, GuardOutcome.PASSED);
		}
		return new AssistantTurn(turn.sessionId(), safeReply, turn.metadata());
	}

}
