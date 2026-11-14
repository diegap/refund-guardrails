package dev.diegap.refundguardrails.policy;

/** What happened to a refund request. Denials never carry a token. */
public sealed interface RefundOutcome {

	record Approved(String refundId, String chargeId, long amountCents) implements RefundOutcome {
	}

	record Denied(DenialReason reason, String message, boolean escalationOffered) implements RefundOutcome {

		static Denied of(DenialReason reason) {
			return new Denied(reason, reason.message(), true);
		}

	}

}
