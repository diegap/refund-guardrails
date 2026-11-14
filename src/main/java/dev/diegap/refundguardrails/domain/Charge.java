package dev.diegap.refundguardrails.domain;

import java.time.Instant;

public record Charge(String id, String accountId, long amountCents, String currency, String description,
		Instant chargedAt) {

	public Charge {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("charge id must not be blank");
		}
		if (accountId == null || accountId.isBlank()) {
			throw new IllegalArgumentException("charge needs an account");
		}
		if (amountCents <= 0) {
			throw new IllegalArgumentException("charge amount must be positive");
		}
	}

}
