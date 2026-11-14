package dev.diegap.refundguardrails.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Finds card numbers using three checks: digit length, a known issuer prefix for
 * that length, and the Luhn checksum. Spaces and dashes are treated as
 * separators. Luhn alone would flag too many order numbers, so the prefix and
 * length checks are not optional.
 */
@Component
public class PanDetector {

	private static final int MIN_DIGITS = 12;

	private static final int MAX_DIGITS = 19;

	private static final Pattern CANDIDATE = Pattern.compile("(?<!\\d)\\d[\\d -]{" + (MIN_DIGITS - 2) + ","
			+ (MAX_DIGITS * 2 - 3) + "}\\d(?!\\d)");

	/**
	 * Returns non-overlapping matches in text order. A number that fails any
	 * check is not reported and must be passed through unchanged by callers.
	 */
	public List<DetectedPan> detect(String text) {
		if (text == null || text.isEmpty()) {
			return List.of();
		}
		List<DetectedPan> pans = new ArrayList<>();
		Matcher matcher = CANDIDATE.matcher(text);
		while (matcher.find()) {
			String digits = matcher.group().replaceAll("[ -]", "");
			if (digits.length() < MIN_DIGITS || digits.length() > MAX_DIGITS) {
				continue;
			}
			if (hasKnownIssuer(digits) && passesLuhn(digits)) {
				pans.add(new DetectedPan(matcher.start(), matcher.end(), digits));
			}
		}
		return List.copyOf(pans);
	}

	static boolean passesLuhn(String digits) {
		int sum = 0;
		boolean doubled = false;
		for (int index = digits.length() - 1; index >= 0; index--) {
			int value = digits.charAt(index) - '0';
			if (doubled) {
				value *= 2;
				if (value > 9) {
					value -= 9;
				}
			}
			sum += value;
			doubled = !doubled;
		}
		return sum % 10 == 0;
	}

	static boolean hasKnownIssuer(String digits) {
		int length = digits.length();
		int firstTwo = prefix(digits, 2);
		int firstThree = prefix(digits, 3);
		int firstFour = prefix(digits, 4);
		int firstSix = prefix(digits, 6);

		if (digits.startsWith("4")) {
			return length == 13 || length == 16 || length == 19;
		}
		if ((firstTwo >= 51 && firstTwo <= 55) || (firstFour >= 2221 && firstFour <= 2720)) {
			return length == 16;
		}
		if (firstTwo == 34 || firstTwo == 37) {
			return length == 15;
		}
		if (digits.startsWith("6011") || firstTwo == 65 || (firstThree >= 644 && firstThree <= 649)
				|| (firstSix >= 622126 && firstSix <= 622925)) {
			return length == 16 || length == 19;
		}
		if ((firstThree >= 300 && firstThree <= 305) || firstTwo == 36 || firstTwo == 38) {
			return length == 14;
		}
		return firstFour >= 3528 && firstFour <= 3589 && length >= 16 && length <= 19;
	}

	private static int prefix(String digits, int length) {
		if (digits.length() < length) {
			return -1;
		}
		return Integer.parseInt(digits.substring(0, length));
	}

}
