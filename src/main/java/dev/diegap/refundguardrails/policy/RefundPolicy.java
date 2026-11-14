package dev.diegap.refundguardrails.policy;

import java.util.Optional;

import dev.diegap.refundguardrails.domain.Charge;
import dev.diegap.refundguardrails.domain.ChargeRepository;
import dev.diegap.refundguardrails.domain.RefundLedger;

/**
 * Deterministic refund policy. A model can ask; only these rules can allow.
 * Every limit comes from configuration or server-side state.
 */
public class RefundPolicy {

	private final ChargeRepository charges;

	private final RefundLedger ledger;

	private final long autoApprovalCapCents;

	public RefundPolicy(ChargeRepository charges, RefundLedger ledger, RefundProperties properties) {
		this(charges, ledger, properties.autoApprovalCapCents());
	}

	RefundPolicy(ChargeRepository charges, RefundLedger ledger, long autoApprovalCapCents) {
		this.charges = charges;
		this.ledger = ledger;
		this.autoApprovalCapCents = autoApprovalCapCents;
	}

	public PolicyDecision evaluate(RefundIntent intent) {
		if (intent.amountCents() <= 0) {
			return PolicyDecision.denied(DenialReason.NON_POSITIVE_AMOUNT);
		}

		Optional<Charge> found = this.charges.chargeById(intent.chargeId());
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

		long refundableBalance = charge.amountCents() - this.ledger.refundedAmountCents(charge.id());
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
