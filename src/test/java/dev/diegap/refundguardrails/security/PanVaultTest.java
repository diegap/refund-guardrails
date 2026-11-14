package dev.diegap.refundguardrails.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PanVaultTest {

	private static final String VISA = "4242424242424242";

	private static final String MASTERCARD = "5555555555554444";

	private final PanVault vault = new PanVault();

	@Test
	void givesACardAStableTokenWithinASession() {
		String first = this.vault.tokenFor("session-a", VISA);
		String second = this.vault.tokenFor("session-a", VISA);

		assertThat(first).isEqualTo("[[CARD_1]]");
		assertThat(second).isEqualTo(first);
		assertThat(this.vault.size("session-a")).isEqualTo(1);
	}

	@Test
	void numbersTokensInFirstUseOrder() {
		assertThat(this.vault.tokenFor("session-a", VISA)).isEqualTo("[[CARD_1]]");
		assertThat(this.vault.tokenFor("session-a", MASTERCARD)).isEqualTo("[[CARD_2]]");
		assertThat(this.vault.size("session-a")).isEqualTo(2);
	}

	@Test
	void resolvesATokenOnlyWithinItsOwnSession() {
		String token = this.vault.tokenFor("session-a", VISA);
		this.vault.tokenFor("session-b", MASTERCARD);

		assertThat(this.vault.digitsFor("session-a", token)).contains(VISA);
		assertThat(this.vault.digitsFor("session-b", token)).contains(MASTERCARD);
		assertThat(this.vault.digitsFor("session-c", token)).isEmpty();
	}

	@Test
	void exposesTheLastFourButNotMore() {
		String token = this.vault.tokenFor("session-a", VISA);

		assertThat(this.vault.lastFour("session-a", token)).contains("4242");
		assertThat(this.vault.digitsFor("session-a", token)).contains(VISA);
	}

	@Test
	void discardsASessionVault() {
		String token = this.vault.tokenFor("session-a", VISA);

		this.vault.discard("session-a");

		assertThat(this.vault.digitsFor("session-a", token)).isEmpty();
		assertThat(this.vault.size("session-a")).isZero();
	}

	@Test
	void restartForgetsEveryMapping() {
		String token = this.vault.tokenFor("session-a", VISA);

		PanVault restarted = new PanVault();

		assertThat(restarted.digitsFor("session-a", token)).isEmpty();
		assertThat(restarted.size("session-a")).isZero();
	}

	@Test
	void unknownOrNullTokensDoNotResolve() {
		assertThat(this.vault.digitsFor("session-a", "[[CARD_9]]")).isEmpty();
		assertThat(this.vault.digitsFor("session-a", null)).isEmpty();
		assertThat(this.vault.lastFour("session-a", "[[CARD_9]]")).isEmpty();
	}

	@Test
	void blankSessionsShareTheDefaultVault() {
		String token = this.vault.tokenFor(null, VISA);

		assertThat(this.vault.digitsFor("default", token)).contains(VISA);
		assertThat(this.vault.size(" ")).isEqualTo(1);
	}

}
