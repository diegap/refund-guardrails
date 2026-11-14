package dev.diegap.refundguardrails.security;

/**
 * One card-number occurrence in a piece of text. {@code start} is inclusive and
 * {@code end} is exclusive, both measured in the original text, so a redactor can
 * replace the exact span including its separators.
 */
public record DetectedPan(int start, int end, String digits) {

	public DetectedPan {
		if (start < 0 || end <= start) {
			throw new IllegalArgumentException("invalid span");
		}
		if (digits == null || digits.length() < 12) {
			throw new IllegalArgumentException("card digits must not be short");
		}
	}

}
