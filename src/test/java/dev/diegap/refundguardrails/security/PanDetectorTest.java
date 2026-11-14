package dev.diegap.refundguardrails.security;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PanDetectorTest {

	private final PanDetector detector = new PanDetector();

	@Test
	void detectsSpacedVisaAndRecordsTheSpan() {
		String text = "Please refund the card 4242 4242 4242 4242 today.";

		List<DetectedPan> pans = this.detector.detect(text);

		assertThat(pans).hasSize(1);
		DetectedPan pan = pans.get(0);
		assertThat(text.substring(pan.start(), pan.end())).isEqualTo("4242 4242 4242 4242");
		assertThat(pan.digits()).isEqualTo("4242424242424242");
	}

	@Test
	void detectsDashedAmexWithItsFifteenDigits() {
		List<DetectedPan> pans = this.detector.detect("Amex: 3782-822463-10005.");

		assertThat(pans).singleElement().satisfies((pan) -> {
			assertThat(pan.digits()).isEqualTo("378282246310005");
			assertThat(pan.start()).isEqualTo(6);
			assertThat(pan.end()).isEqualTo(23);
		});
	}

	@Test
	void detectsCompactMastercard() {
		List<DetectedPan> pans = this.detector.detect("card 5555555555554444 expired");

		assertThat(pans).singleElement()
			.satisfies((pan) -> assertThat(pan.digits()).isEqualTo("5555555555554444"));
	}

	@Test
	void leavesANumberThatFailsLuhnAlone() {
		assertThat(this.detector.detect("order 4242 4242 4242 4241 shipped")).isEmpty();
	}

	@Test
	void leavesAnOrderIdentifierAlone() {
		assertThat(this.detector.detect("ORDER-20241114001 is late")).isEmpty();
		assertThat(this.detector.detect("ORDER-1234567890123456 is late")).isEmpty();
	}

	@Test
	void leavesALuhnValidNumberWithoutAKnownIssuerAlone() {
		assertThat(this.detector.detect("reference 0000 0000 0000 0000")).isEmpty();
	}

	@Test
	void findsMultipleCardsInTextOrder() {
		List<DetectedPan> pans = this.detector.detect(
				"first 4242 4242 4242 4242 then 378282246310005 and later 5555 5555 5555 4444");

		assertThat(pans).extracting(DetectedPan::digits)
			.containsExactly("4242424242424242", "378282246310005", "5555555555554444");
	}

	@Test
	void handlesEmptyInput() {
		assertThat(this.detector.detect(null)).isEmpty();
		assertThat(this.detector.detect("")).isEmpty();
		assertThat(this.detector.detect("no numbers here")).isEmpty();
	}

}
