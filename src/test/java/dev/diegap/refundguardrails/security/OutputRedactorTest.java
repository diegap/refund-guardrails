package dev.diegap.refundguardrails.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OutputRedactorTest {

	private final OutputRedactor redactor = new OutputRedactor(new PanDetector());

	@Test
	void masksACardTheModelEchoed() {
		RedactedText result = this.redactor.redact(
				"I refunded the card 4242 4242 4242 4242 for you.");

		assertThat(result.text()).isEqualTo("I refunded the card [REDACTED_CARD] for you.");
		assertThat(result.text()).doesNotContain("4242 4242 4242 4242");
		assertThat(result.redactions()).containsExactly(
				new Redaction(RedactionStage.OUTPUT, RedactionKind.CARD_NUMBER, null, "4242"));
	}

	@Test
	void masksAnEmailAddress() {
		RedactedText result = this.redactor.redact("We will email ada@example.com tonight.");

		assertThat(result.text()).isEqualTo("We will email [REDACTED_EMAIL] tonight.");
		assertThat(result.redactions()).containsExactly(
				new Redaction(RedactionStage.OUTPUT, RedactionKind.EMAIL_ADDRESS, null, null));
	}

	@Test
	void masksSeveralValuesInOneOutput() {
		RedactedText result = this.redactor.redact(
				"Card 5555555555554444 failed; write to grace@example.com.");

		assertThat(result.text()).isEqualTo("Card [REDACTED_CARD] failed; write to [REDACTED_EMAIL].");
		assertThat(result.redactions()).extracting(Redaction::kind)
			.containsExactly(RedactionKind.CARD_NUMBER, RedactionKind.EMAIL_ADDRESS);
		assertThat(result.redactions().get(0).lastFour()).isEqualTo("4444");
	}

	@Test
	void leavesCleanOutputByteIdentical() {
		String clean = "Your refund of $42.00 is on the way. Contact us if it does not arrive.";

		RedactedText result = this.redactor.redact(clean);

		assertThat(result.text()).isEqualTo(clean);
		assertThat(result.hasRedactions()).isFalse();
	}

	@Test
	void leavesAshortDigitReferenceAlone() {
		assertThat(this.redactor.redact("Refund rf_demo_1 covers ch_demo_1 in 7 days.").text())
			.isEqualTo("Refund rf_demo_1 covers ch_demo_1 in 7 days.");
	}

}
