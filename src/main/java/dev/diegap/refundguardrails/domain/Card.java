package dev.diegap.refundguardrails.domain;

/**
 * A payment card as held by the demo domain. The full number exists in memory so
 * the vault can map it, but it never leaves the domain or security packages:
 * everything model-facing receives a {@link CardSummary}.
 */
public record Card(String pan, String lastFour, String brand) {

	public Card {
		if (pan == null || pan.length() < 12) {
			throw new IllegalArgumentException("a card needs a plausible number");
		}
		if (lastFour == null || lastFour.length() != 4) {
			throw new IllegalArgumentException("last four must be exactly four digits");
		}
	}

	public CardSummary summary() {
		return new CardSummary(this.lastFour, this.brand);
	}

}
