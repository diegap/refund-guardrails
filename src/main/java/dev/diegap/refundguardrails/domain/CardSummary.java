package dev.diegap.refundguardrails.domain;

/**
 * The only card representation allowed to cross into model-facing code: a
 * brand and four digits. {@link #toString()} is deliberately limited to the
 * same data.
 */
public record CardSummary(String lastFour, String brand) {

	public static CardSummary of(Card card) {
		return card.summary();
	}

}
