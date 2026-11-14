package dev.diegap.refundguardrails.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InputRedactorTest {

	private final PanVault vault = new PanVault();

	private final InputRedactor redactor = new InputRedactor(new PanDetector(), this.vault);

	@Test
	void replacesACardWithASessionToken() {
		RedactedText result = this.redactor.redact("session-a",
				"Refund the card 4242 4242 4242 4242, please.");

		assertThat(result.text()).isEqualTo("Refund the card [[CARD_1]], please.");
		assertThat(result.text()).doesNotContain("4242 4242 4242 4242");
		assertThat(result.redactions()).containsExactly(
				new Redaction(RedactionStage.INPUT, RedactionKind.CARD_NUMBER, "[[CARD_1]]", "4242"));
	}

	@Test
	void reusesTheSameTokenForTheSameCardAcrossTurns() {
		String first = this.redactor.redact("session-a", "card 4242 4242 4242 4242").text();
		String second = this.redactor.redact("session-a", "same card: 4242-4242-4242-4242").text();

		assertThat(first).isEqualTo("card [[CARD_1]]");
		assertThat(second).isEqualTo("same card: [[CARD_1]]");
		assertThat(this.vault.size("session-a")).isEqualTo(1);
	}

	@Test
	void tokensAreScopedToTheSession() {
		String first = this.redactor.redact("session-a", "4242 4242 4242 4242").text();
		String second = this.redactor.redact("session-b", "4242 4242 4242 4242").text();

		assertThat(first).isEqualTo(second).isEqualTo("[[CARD_1]]");
		assertThat(this.vault.digitsFor("session-a", "[[CARD_1]]")).contains("4242424242424242");
		assertThat(this.vault.digitsFor("session-c", "[[CARD_1]]")).isEmpty();
	}

	@Test
	void numbersMultipleCardsInTextOrder() {
		RedactedText result = this.redactor.redact("session-a",
				"visa 4242 4242 4242 4242, amex 378282246310005");

		assertThat(result.text()).isEqualTo("visa [[CARD_1]], amex [[CARD_2]]");
		assertThat(result.redactions()).extracting(Redaction::lastFour).containsExactly("4242", "0005");
	}

	@Test
	void leavesNonCardNumbersUnchanged() {
		String text = "ORDER-20241114001 failed; retry reference 1234567890123456.";

		RedactedText result = this.redactor.redact("session-a", text);

		assertThat(result.text()).isEqualTo(text);
		assertThat(result.hasRedactions()).isFalse();
	}

	@Test
	void leavesEmailAddressesForTheOutputStage() {
		RedactedText result = this.redactor.redact("session-a",
				"Email ada@example.com about card 4242 4242 4242 4242");

		assertThat(result.text()).isEqualTo("Email ada@example.com about card [[CARD_1]]");
	}

}
