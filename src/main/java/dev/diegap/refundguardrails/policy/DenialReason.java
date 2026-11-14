package dev.diegap.refundguardrails.policy;

/** Why a refund was refused. Categories only: no amounts, cards, or tokens. */
public enum DenialReason {

	NON_POSITIVE_AMOUNT("A refund needs a positive amount. I can escalate this to a human agent."),
	CHARGE_NOT_FOUND("I could not find that charge. I can escalate this to a human agent."),
	ACCOUNT_MISMATCH("That charge does not belong to the account in this conversation. I can escalate this to a human agent."),
	CHARGE_NOT_REFUNDABLE("That charge is not refundable. I can escalate this to a human agent."),
	ALREADY_REFUNDED("That charge has already been refunded. I can escalate this to a human agent."),
	EXCEEDS_CHARGE("That amount is higher than the refundable balance of the charge. I can escalate this to a human agent."),
	ABOVE_AUTO_APPROVAL_CAP("That refund is above the automatic approval limit. I can escalate this to a human agent."),
	AUTHORIZATION_INVALID("I could not verify that refund authorization. I can escalate this to a human agent.");

	private final String message;

	DenialReason(String message) {
		this.message = message;
	}

	public String message() {
		return this.message;
	}

}
