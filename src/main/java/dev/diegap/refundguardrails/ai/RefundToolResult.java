package dev.diegap.refundguardrails.ai;

/**
 * The result of a refund request. A denial carries a category and a safe
 * explanation, never an authorization token or a card value.
 */
public record RefundToolResult(String status, String refundId, String chargeId, long amountCents, String reason,
		String message, boolean escalationOffered) {

	public static RefundToolResult approved(String refundId, String chargeId, long amountCents) {
		return new RefundToolResult("approved", refundId, chargeId, amountCents, null,
				"The refund was approved and recorded.", false);
	}

	public static RefundToolResult denied(String chargeId, long amountCents, String reason, String message) {
		return new RefundToolResult("denied", null, chargeId, amountCents, reason, message, true);
	}

}
