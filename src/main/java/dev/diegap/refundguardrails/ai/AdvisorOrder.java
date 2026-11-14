package dev.diegap.refundguardrails.ai;

/**
 * Explicit advisor order. Lower runs earlier on the way in and later on the way
 * out. The tool-calling advisor is configured at order 1000
 * ({@code spring.ai.chat.client.tool-calling.advisor-order}), so these four wrap
 * the whole tool loop exactly once per turn.
 */
public final class AdvisorOrder {

	public static final int CARD_REDACTION = 100;

	public static final int PROMPT_INJECTION = 200;

	public static final int TOPIC_SCOPE = 300;

	public static final int OUTPUT_SCANNING = 400;

	private AdvisorOrder() {
	}

}
