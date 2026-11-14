package dev.diegap.refundguardrails.assistant;

import java.util.Map;

import dev.diegap.refundguardrails.security.OutputRedactor;
import dev.diegap.refundguardrails.security.RedactedText;
import io.micrometer.observation.Observation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.stereotype.Service;

/**
 * One conversational turn. Input guards run inside the advisor chain; this
 * service supplies the per-turn context, turns failures into safe answers, and
 * returns the metadata the interface needs.
 */
@Service
public class RefundAssistantService {

	static final String SAFE_PROVIDER_FAILURE = "The assistant is temporarily unavailable. "
			+ "Please try again or ask for a human agent.";

	static final String SAFE_TOOL_FAILURE = "I couldn't complete that action. "
			+ "Please try again or ask to speak with a human agent.";

	private static final Logger log = LoggerFactory.getLogger(RefundAssistantService.class);

	private final ChatClient chatClient;

	private final OutputRedactor outputRedactor;

	private final TurnObservation observations;

	public RefundAssistantService(ChatClient refundChatClient, OutputRedactor outputRedactor,
			TurnObservation observations) {
		this.chatClient = refundChatClient;
		this.outputRedactor = outputRedactor;
		this.observations = observations;
	}

	public AssistantTurn chat(AssistantTurnRequest request) {
		TurnContext turn = new TurnContext(request.sessionId(), request.message());
		Observation observation = this.observations.start(request);
		observation.start();
		try (Observation.Scope scope = observation.openScope()) {
			return doChat(request, turn);
		}
		finally {
			this.observations.annotate(observation, turn.metadata());
			observation.stop();
		}
	}

	private AssistantTurn doChat(AssistantTurnRequest request, TurnContext turn) {
		try {
			ChatClientResponse response = this.chatClient.prompt()
				.user(modelInput(request))
				.advisors((advisors) -> advisors.param(TurnContext.KEY, turn))
				.toolContext(Map.of(TurnContext.KEY, turn))
				.call()
				.chatClientResponse();

			String reply = turn.finalReply();
			if (reply == null) {
				reply = scan(turn, textOf(response));
			}
			return new AssistantTurn(turn.sessionId(), reply, turn.metadata());
		}
		catch (RuntimeException ex) {
			if (causedByToolFailure(ex)) {
				log.warn("tool failure in session {}: {}", turn.sessionId(), ex.getClass().getSimpleName());
				return safeFailure(turn, FailureCategory.TOOL_ERROR, SAFE_TOOL_FAILURE);
			}
			log.warn("provider failure in session {}: {}", turn.sessionId(), ex.getClass().getSimpleName());
			return safeFailure(turn, FailureCategory.PROVIDER_ERROR, SAFE_PROVIDER_FAILURE);
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

	private static boolean causedByToolFailure(Throwable failure) {
		for (Throwable current = failure; current != null; current = current.getCause()) {
			if (current instanceof ToolExecutionException) {
				return true;
			}
			if (current instanceof IllegalStateException && current.getMessage() != null
					&& current.getMessage().toLowerCase(java.util.Locale.ROOT).contains("tool")) {
				return true;
			}
			if (current.getCause() == current) {
				break;
			}
		}
		return false;
	}

	private static String textOf(ChatClientResponse response) {
		if (response == null || response.chatResponse() == null || response.chatResponse().getResult() == null) {
			return "";
		}
		String text = response.chatResponse().getResult().getOutput().getText();
		return text == null ? "" : text;
	}

}
