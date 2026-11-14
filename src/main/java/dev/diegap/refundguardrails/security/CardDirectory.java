package dev.diegap.refundguardrails.security;

import java.util.Optional;

import dev.diegap.refundguardrails.domain.CardSummary;
import dev.diegap.refundguardrails.domain.DemoData;
import org.springframework.stereotype.Component;

/**
 * Resolves a vault token to the only card shape allowed in tool results: brand
 * plus last four. It deliberately has no method that returns a full number.
 */
@Component
public class CardDirectory {

	private final PanVault vault;

	private final DemoData demoData;

	public CardDirectory(PanVault vault, DemoData demoData) {
		this.vault = vault;
		this.demoData = demoData;
	}

	public Optional<CardSummary> resolve(String sessionId, String token) {
		return this.vault.digitsFor(sessionId, token)
			.flatMap(this.demoData::accountForPan)
			.map((account) -> account.card().summary());
	}

}
