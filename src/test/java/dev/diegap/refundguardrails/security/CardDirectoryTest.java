package dev.diegap.refundguardrails.security;

import java.util.Optional;

import dev.diegap.refundguardrails.domain.CardSummary;
import dev.diegap.refundguardrails.store.DemoData;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CardDirectoryTest {

	private final PanVault vault = new PanVault();

	private final CardDirectory directory = new CardDirectory(this.vault, new DemoData());

	@Test
	void resolvesATokenToBrandAndLastFourOnly() {
		String token = this.vault.tokenFor("session-a", "4242424242424242");

		Optional<CardSummary> summary = this.directory.resolve("session-a", token);

		assertThat(summary).contains(new CardSummary("4242", "Visa"));
	}

	@Test
	void aCardSummaryNeverCarriesMoreThanFourConsecutiveDigits() {
		for (String pan : new String[] { "4242424242424242", "5555555555554444", "378282246310005" }) {
			String token = this.vault.tokenFor("session-a", pan);
			CardSummary summary = this.directory.resolve("session-a", token).orElseThrow();

			assertThat(summary.toString()).doesNotContainPattern("\\d{5,}");
			assertThat(summary.lastFour()).hasSize(4);
		}
	}

	@Test
	void doesNotResolveATokenFromAnotherSession() {
		String token = this.vault.tokenFor("session-a", "4242424242424242");

		assertThat(this.directory.resolve("session-b", token)).isEmpty();
	}

	@Test
	void doesNotResolveAnUnknownTokenOrCard() {
		assertThat(this.directory.resolve("session-a", "[[CARD_9]]")).isEmpty();
		assertThat(this.directory.resolve("session-a", this.vault.tokenFor("session-a", "4111111111111111")))
			.isEmpty();
	}

}
