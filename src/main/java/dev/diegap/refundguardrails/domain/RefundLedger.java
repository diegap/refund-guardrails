package dev.diegap.refundguardrails.domain;

import java.util.List;

/**
 * Records refunds and reports how much has already been refunded per charge.
 * The in-memory implementation lives outside the domain.
 */
public interface RefundLedger {

	void record(Refund refund);

	List<Refund> refunds();

	List<Refund> refundsFor(String chargeId);

	long refundedAmountCents(String chargeId);

	/** Clears the ledger; used to isolate tests and eval cases. */
	void clear();

}
