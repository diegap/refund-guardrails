package dev.diegap.refundguardrails.store;

import java.time.Instant;

import dev.diegap.refundguardrails.domain.Refund;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryRefundLedgerTest {

	private final InMemoryRefundLedger ledger = new InMemoryRefundLedger();

	@Test
	void startsEmptyAndTotalsRefundsPerCharge() {
		assertThat(this.ledger.refunds()).isEmpty();
		assertThat(this.ledger.refundedAmountCents("ch_demo_1")).isZero();

		this.ledger.record(new Refund("rf_demo_1", "ch_demo_1", 1200, Instant.EPOCH));
		this.ledger.record(new Refund("rf_demo_2", "ch_demo_1", 300, Instant.EPOCH));

		assertThat(this.ledger.refundedAmountCents("ch_demo_1")).isEqualTo(1500);
		assertThat(this.ledger.refundedAmountCents("ch_demo_0")).isZero();
	}

}
