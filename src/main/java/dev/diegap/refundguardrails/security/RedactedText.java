package dev.diegap.refundguardrails.security;

import java.util.List;

public record RedactedText(String text, List<Redaction> redactions) {

	public RedactedText {
		redactions = redactions == null ? List.of() : List.copyOf(redactions);
	}

	public static RedactedText unchanged(String text) {
		return new RedactedText(text, List.of());
	}

	public boolean hasRedactions() {
		return !this.redactions.isEmpty();
	}

}
