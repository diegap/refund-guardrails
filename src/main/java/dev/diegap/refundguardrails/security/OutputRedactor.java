package dev.diegap.refundguardrails.security;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Masks card numbers and email addresses in model output before delivery. Output
 * masking is a backstop: the model should never see a full card number in the
 * first place, but a prompt-injected or misbehaving model could echo one.
 */
@Component
public class OutputRedactor {

	public static final String CARD_MARKER = "[REDACTED_CARD]";

	public static final String EMAIL_MARKER = "[REDACTED_EMAIL]";

	private static final Pattern EMAIL = Pattern
		.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

	private final PanDetector detector;

	public OutputRedactor(PanDetector detector) {
		this.detector = detector;
	}

	public RedactedText redact(String text) {
		if (text == null || text.isEmpty()) {
			return RedactedText.unchanged(text);
		}

		List<Redaction> redactions = new ArrayList<>();
		String withoutCards = maskCards(text, redactions);
		String withoutEmails = maskEmails(withoutCards, redactions);
		return redactions.isEmpty() ? RedactedText.unchanged(text)
				: new RedactedText(withoutEmails, redactions);
	}

	private String maskCards(String text, List<Redaction> redactions) {
		List<DetectedPan> pans = this.detector.detect(text);
		if (pans.isEmpty()) {
			return text;
		}
		StringBuilder masked = new StringBuilder(text.length());
		int cursor = 0;
		for (DetectedPan pan : pans) {
			masked.append(text, cursor, pan.start());
			masked.append(CARD_MARKER);
			redactions.add(Redaction.outputCard(pan.digits().substring(pan.digits().length() - 4)));
			cursor = pan.end();
		}
		masked.append(text, cursor, text.length());
		return masked.toString();
	}

	private String maskEmails(String text, List<Redaction> redactions) {
		Matcher matcher = EMAIL.matcher(text);
		StringBuilder masked = new StringBuilder(text.length());
		int cursor = 0;
		while (matcher.find()) {
			masked.append(text, cursor, matcher.start());
			masked.append(EMAIL_MARKER);
			redactions.add(Redaction.outputEmail());
			cursor = matcher.end();
		}
		if (cursor == 0) {
			return text;
		}
		masked.append(text, cursor, text.length());
		return masked.toString();
	}

}
