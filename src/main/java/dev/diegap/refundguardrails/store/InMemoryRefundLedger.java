package dev.diegap.refundguardrails.store;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import dev.diegap.refundguardrails.domain.Refund;
import dev.diegap.refundguardrails.domain.RefundLedger;
import org.springframework.stereotype.Component;

/**
 * In-memory refund ledger. State is process-local and non-durable, which keeps
 * the demo to one command and makes eval isolation simple.
 */
@Component
public class InMemoryRefundLedger implements RefundLedger {

	private final List<Refund> refunds = new CopyOnWriteArrayList<>();

	@Override
	public void record(Refund refund) {
		this.refunds.add(refund);
	}

	@Override
	public List<Refund> refunds() {
		return List.copyOf(this.refunds);
	}

	@Override
	public List<Refund> refundsFor(String chargeId) {
		return this.refunds.stream().filter((refund) -> refund.chargeId().equals(chargeId)).toList();
	}

	@Override
	public long refundedAmountCents(String chargeId) {
		return refundsFor(chargeId).stream().mapToLong(Refund::amountCents).sum();
	}

	@Override
	public void clear() {
		this.refunds.clear();
	}

}
