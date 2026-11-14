package dev.diegap.refundguardrails.assistant;

/** Safe replies the application returns when a turn fails technically. */
public final class AssistantReplies {

	public static final String SAFE_PROVIDER_FAILURE = "The assistant is temporarily unavailable. "
			+ "Please try again or ask for a human agent.";

	public static final String SAFE_TOOL_FAILURE = "I couldn't complete that action. "
			+ "Please try again or ask to speak with a human agent.";

	private AssistantReplies() {
	}

}
