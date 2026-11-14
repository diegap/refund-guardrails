package dev.diegap.refundguardrails.assistant;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * A keyword scope check: the assistant only handles money-support topics.
 * Off-topic text is refused before any model call. This is a product guard, not
 * a security boundary.
 */
@Component
public final class TopicScope {

	private static final Pattern IN_SCOPE = Pattern.compile(
			"\\b(accounts?|cards?|charges?|charging|charged|refunds?|refunded|refunding|payments?|paid|pay|"
					+ "billing|bills?|invoices?|receipts?|transactions?|subscriptions?|plans?|duplicates?|"
					+ "twice|overcharged|money|dollars?|cents?|escalate|escalation|human|agents?|support|"
					+ "purchases?|orders?|merchants?)\\b",
			Pattern.CASE_INSENSITIVE);

	public boolean isInScope(String text) {
		return text != null && IN_SCOPE.matcher(text).find();
	}

}
