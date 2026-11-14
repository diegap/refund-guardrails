package dev.diegap.refundguardrails.policy;

import java.util.Optional;

import dev.diegap.refundguardrails.domain.Charge;
import dev.diegap.refundguardrails.domain.DemoData;

/**
 * Deterministic refund policy. A model can ask; only these rules can allow.
 * Every limit comes from configuration or server-side state.
 */
public class RefundPolicy {

	private final DemoData data;

	private final long autoApprovalCapCents;

	public RefundPolicy(DemoData data, RefundProperties properties) {
		this(data, properties.autoApprovalCapCents());
	}

	RefundPolicy(DemoData data, long autoApprovalCapCents) {
		this.data = data;
		this.autoApprovalCapCents = autoApprovalCapCents;
	}

	public PolicyDecision evaluate(RefundIntent intent) {
		if (intent.amountCents() <= 0) {
			return PolicyDecision.denied(DenialReason.NON_POSITIVE_AMOUNT);
		}

		Optional<Charge> found = this.data.charge(intent.chargeId());
		if (found.isEmpty()) {
			return PolicyDecision.denied(DenialReason.CHARGE_NOT_FOUND);
		}
		Charge charge = found.get();
		if (!charge.accountId().equals(intent.accountId())) {
			return PolicyDecision.denied(DenialReason.ACCOUNT_MISMATCH);
		}
		if (!charge.refundable()) {
			return PolicyDecision.denied(DenialReason.CHARGE_NOT_REFUNDABLE);
		}

		long refundableBalance = charge.amountCents() - this.data.ledger().refundedAmountCents(charge.id());
		if (refundableBalance <= 0) {
			return PolicyDecision.denied(DenialReason.ALREADY_REFUNDED);
		}
		if (intent.amountCents() > refundableBalance) {
			return PolicyDecision.denied(DenialReason.EXCEEDS_CHARGE);
		}
		if (intent.amountCents() > this.autoApprovalCapCents) {
			return PolicyDecision.denied(DenialReason.ABOVE_AUTO_APPROVAL_CAP);
		}
		return PolicyDecision.allowed();
	}

}
