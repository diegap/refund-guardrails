package dev.diegap.refundguardrails.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Replaces card numbers in user input with per-session vault tokens. This runs
 * before any model-bound content exists, so the panic value is never serialized
 * into a request, a span, or a log line.
 */
@Component
public class InputRedactor {

	private final PanDetector detector;

	private final PanVault vault;

	public InputRedactor(PanDetector detector, PanVault vault) {
		this.detector = detector;
		this.vault = vault;
	}

	public RedactedText redact(String sessionId, String text) {
		List<DetectedPan> pans = this.detector.detect(text);
		if (pans.isEmpty()) {
			return RedactedText.unchanged(text);
		}

		StringBuilder redacted = new StringBuilder(text.length());
		List<Redaction> redactions = new ArrayList<>(pans.size());
		int cursor = 0;
		for (DetectedPan pan : pans) {
			redacted.append(text, cursor, pan.start());
			String token = this.vault.tokenFor(sessionId, pan.digits());
			redacted.append(token);
			redactions.add(Redaction.inputCard(token, lastFour(pan.digits())));
			cursor = pan.end();
		}
		redacted.append(text, cursor, text.length());
		return new RedactedText(redacted.toString(), redactions);
	}

	private static String lastFour(String digits) {
		return digits.substring(digits.length() - 4);
	}

}
