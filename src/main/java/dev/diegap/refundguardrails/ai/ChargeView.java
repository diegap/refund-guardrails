package dev.diegap.refundguardrails.ai;

/**
 * Model-facing charge projection. The field set is deliberately limited to what
 * the assistant needs: identifier, amount, merchant, date, status, and card last
 * four.
 */
public record ChargeView(String chargeId, long amountCents, String currency, String merchant, String chargedAt,
		String status, String cardLastFour) {
}
