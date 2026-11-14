package dev.diegap.refundguardrails.testsupport;

import java.util.Set;

/**
 * Industry test card numbers, kept independent of production code so the seed
 * test can prove it did not just agree with the detector. Each entry is
 * documented as a test number by its issuer or by a payment provider.
 */
public final class DocumentedTestPans {

	public static final Set<String> ALL = Set.of(
			// Visa test number, docs.stripe.com/testing
			"4242424242424242",
			// Mastercard test number, docs.stripe.com/testing
			"5555555555554444",
			// American Express test number, docs.stripe.com/testing
			"378282246310005");

	private DocumentedTestPans() {
	}

	public static boolean isDocumented(String pan) {
		return ALL.contains(pan);
	}

	/** Independent Luhn implementation, so the test does not reuse production logic. */
	public static boolean passesLuhn(String digits) {
		int sum = 0;
		boolean doubled = false;
		for (int index = digits.length() - 1; index >= 0; index--) {
			int value = digits.charAt(index) - '0';
			if (doubled) {
				value *= 2;
				sum += value > 9 ? value - 9 : value;
			}
			else {
				sum += value;
			}
			doubled = !doubled;
		}
		return sum % 10 == 0;
	}

}
