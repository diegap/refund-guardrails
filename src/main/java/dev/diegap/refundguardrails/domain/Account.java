package dev.diegap.refundguardrails.domain;

public record Account(String id, String holderName, Card card) {

	public Account {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("account id must not be blank");
		}
		if (card == null) {
			throw new IllegalArgumentException("account needs a card");
		}
	}

}
