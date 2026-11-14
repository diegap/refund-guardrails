package dev.diegap.refundguardrails.policy;

/** The outcome of evaluating a {@link RefundIntent} against server-side state. */
public sealed interface PolicyDecision {

	record Allowed() implements PolicyDecision {
	}

	record Denied(DenialReason reason) implements PolicyDecision {
	}

	static PolicyDecision allowed() {
		return new Allowed();
	}

	static PolicyDecision denied(DenialReason reason) {
		return new Denied(reason);
	}

}
