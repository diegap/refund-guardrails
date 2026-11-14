package dev.diegap.refundguardrails.domain;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory refund ledger. Group 5 adds enforcement; the domain only records and
 * totals refunds here.
 */
public class RefundLedger {

	private final List<Refund> refunds = new CopyOnWriteArrayList<>();

	public void record(Refund refund) {
		this.refunds.add(refund);
	}

	public List<Refund> refunds() {
		return List.copyOf(this.refunds);
	}

	public List<Refund> refundsFor(String chargeId) {
		return this.refunds.stream().filter((refund) -> refund.chargeId().equals(chargeId)).toList();
	}

	public long refundedAmountCents(String chargeId) {
		return refundsFor(chargeId).stream().mapToLong(Refund::amountCents).sum();
	}

}
