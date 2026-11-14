package dev.diegap.refundguardrails.security;

/**
 * One redaction, reported to the caller. {@code token} is present for input
 * card substitutions and absent for output masking; {@code lastFour} is present
 * only when the redacted value was a card number.
 */
public record Redaction(RedactionStage stage, RedactionKind kind, String token, String lastFour) {

	public static Redaction inputCard(String token, String lastFour) {
		return new Redaction(RedactionStage.INPUT, RedactionKind.CARD_NUMBER, token, lastFour);
	}

	public static Redaction outputCard(String lastFour) {
		return new Redaction(RedactionStage.OUTPUT, RedactionKind.CARD_NUMBER, null, lastFour);
	}

	public static Redaction outputEmail() {
		return new Redaction(RedactionStage.OUTPUT, RedactionKind.EMAIL_ADDRESS, null, null);
	}

}
