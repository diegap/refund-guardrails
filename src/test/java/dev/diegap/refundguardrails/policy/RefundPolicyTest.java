package dev.diegap.refundguardrails.policy;

import java.time.Instant;

import dev.diegap.refundguardrails.infrastructure.DemoData;
import dev.diegap.refundguardrails.domain.Refund;
import dev.diegap.refundguardrails.domain.RefundLedger;
import dev.diegap.refundguardrails.infrastructure.InMemoryRefundLedger;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefundPolicyTest {

	private static final long CAP = 25000;

	private final DemoData data = new DemoData();

	private final RefundLedger ledger = new InMemoryRefundLedger();

	private final RefundPolicy policy = new RefundPolicy(this.data, this.ledger, CAP);

	private RefundIntent intent(String accountId, String chargeId, long amountCents) {
		return new RefundIntent("session-a", accountId, chargeId, amountCents);
	}

	@Test
	void allowsARefundWithinTheCap() {
		assertThat(this.policy.evaluate(intent("acct_demo_1", "ch_demo_1", 4200)))
			.isInstanceOf(PolicyDecision.Allowed.class);
	}

	@Test
	void allowsARefundExactlyAtTheCap() {
		RefundPolicy atCap = new RefundPolicy(this.data, this.ledger, 50000);

		assertThat(atCap.evaluate(intent("acct_demo_2", "ch_demo_over_cap", 50000)))
			.isInstanceOf(PolicyDecision.Allowed.class);
	}

	@Test
	void refusesARefundAboveTheCap() {
		assertThat(this.policy.evaluate(intent("acct_demo_2", "ch_demo_over_cap", 30000)))
			.isEqualTo(PolicyDecision.denied(DenialReason.ABOVE_AUTO_APPROVAL_CAP));
	}

	@Test
	void refusesAnAmountAboveTheCharge() {
		assertThat(this.policy.evaluate(intent("acct_demo_1", "ch_demo_1", 4201)))
			.isEqualTo(PolicyDecision.denied(DenialReason.EXCEEDS_CHARGE));
	}

	@Test
	void refusesOnlyTheRefundableBalanceAfterAPartialRefund() {
		this.ledger.record(new Refund("rf_test_1", "ch_demo_3", 1000, Instant.EPOCH));

		assertThat(this.policy.evaluate(intent("acct_demo_3", "ch_demo_3", 1500)))
			.isInstanceOf(PolicyDecision.Allowed.class);
		assertThat(this.policy.evaluate(intent("acct_demo_3", "ch_demo_3", 1501)))
			.isEqualTo(PolicyDecision.denied(DenialReason.EXCEEDS_CHARGE));
	}

	@Test
	void refusesNonPositiveAmounts() {
		assertThat(this.policy.evaluate(intent("acct_demo_1", "ch_demo_1", 0)))
			.isEqualTo(PolicyDecision.denied(DenialReason.NON_POSITIVE_AMOUNT));
		assertThat(this.policy.evaluate(intent("acct_demo_1", "ch_demo_1", -1)))
			.isEqualTo(PolicyDecision.denied(DenialReason.NON_POSITIVE_AMOUNT));
	}

	@Test
	void refusesAChargeThatBelongsToAnotherAccount() {
		assertThat(this.policy.evaluate(intent("acct_demo_2", "ch_demo_1", 4200)))
			.isEqualTo(PolicyDecision.denied(DenialReason.ACCOUNT_MISMATCH));
	}

	@Test
	void refusesAnUnknownCharge() {
		assertThat(this.policy.evaluate(intent("acct_demo_1", "ch_missing", 100)))
			.isEqualTo(PolicyDecision.denied(DenialReason.CHARGE_NOT_FOUND));
	}

	@Test
	void refusesANonRefundableCharge() {
		assertThat(this.policy.evaluate(intent("acct_demo_3", "ch_demo_final_sale", 100)))
			.isEqualTo(PolicyDecision.denied(DenialReason.CHARGE_NOT_REFUNDABLE));
	}

	@Test
	void refusesAFullyRefundedCharge() {
		this.ledger.record(new Refund("rf_test_1", "ch_demo_1", 4200, Instant.EPOCH));

		assertThat(this.policy.evaluate(intent("acct_demo_1", "ch_demo_1", 100)))
			.isEqualTo(PolicyDecision.denied(DenialReason.ALREADY_REFUNDED));
	}

}
