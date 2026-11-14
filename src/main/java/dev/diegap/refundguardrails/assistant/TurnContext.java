package dev.diegap.refundguardrails.assistant;

import java.util.ArrayList;
import java.util.List;

import dev.diegap.refundguardrails.security.Redaction;
import org.springframework.ai.chat.client.ChatClientRequest;

/**
 * Mutable per-turn state carried through the advisor chain and into the tools via
 * {@link org.springframework.ai.chat.model.ToolContext}. It is created fresh for
 * every turn, so no state leaks between sessions.
 */
public final class TurnContext {

	public static final String KEY = "refundGuardrails.turn";

	public static final String CARD_REDACTION = "card-number-redaction";

	public static final String PROMPT_INJECTION = "prompt-injection";

	public static final String TOPIC_SCOPE = "topic-scope";

	public static final String OUTPUT_SCANNING = "output-scanning";

	private final String sessionId;

	private final String rawInput;

	private final List<GuardEvaluation> guards = new ArrayList<>();

	private final List<String> toolsInvoked = new ArrayList<>();

	private final List<Redaction> redactions = new ArrayList<>();

	private String modelBoundInput;

	private String finalReply;

	private FailureCategory failureCategory = FailureCategory.NONE;

	public TurnContext(String sessionId, String rawInput) {
		this.sessionId = sessionId;
		this.rawInput = rawInput;
	}

	public static TurnContext require(ChatClientRequest request) {
		Object value = request.context().get(KEY);
		if (value instanceof TurnContext context) {
			return context;
		}
		throw new IllegalStateException("advisors require a turn context parameter");
	}

	public String sessionId() {
		return this.sessionId;
	}

	public String rawInput() {
		return this.rawInput;
	}

	public void guard(String name, GuardOutcome outcome) {
		this.guards.add(new GuardEvaluation(name, outcome, null));
	}

	public void guard(String name, GuardOutcome outcome, String reason) {
		this.guards.add(new GuardEvaluation(name, outcome, reason));
	}

	public boolean hasGuard(String name) {
		return this.guards.stream().anyMatch((evaluation) -> evaluation.name().equals(name));
	}

	public void toolInvoked(String name) {
		this.toolsInvoked.add(name);
	}

	public void addRedactions(List<Redaction> additions) {
		if (additions != null) {
			this.redactions.addAll(additions);
		}
	}

	public void modelBoundInput(String text) {
		this.modelBoundInput = text;
	}

	public void finalReply(String text) {
		this.finalReply = text;
	}

	public String finalReply() {
		return this.finalReply;
	}

	public void fail(FailureCategory category) {
		this.failureCategory = category;
	}

	public TurnMetadata metadata() {
		return new TurnMetadata(this.rawInput, this.modelBoundInput, this.guards, this.toolsInvoked,
				this.redactions, this.failureCategory);
	}

}
