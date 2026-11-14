package dev.diegap.refundguardrails.domain;

import java.time.Instant;

public record Refund(String id, String chargeId, long amountCents, Instant createdAt) {

	public Refund {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("refund id must not be blank");
		}
		if (amountCents <= 0) {
			throw new IllegalArgumentException("refund amount must be positive");
		}
	}

}
