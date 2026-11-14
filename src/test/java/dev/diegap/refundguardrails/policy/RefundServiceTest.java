package dev.diegap.refundguardrails.policy;

import java.time.Duration;
import java.time.Instant;

import dev.diegap.refundguardrails.domain.DemoData;
import dev.diegap.refundguardrails.domain.Refund;
import dev.diegap.refundguardrails.testsupport.MutableClock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefundServiceTest {

	private final DemoData data = new DemoData();

	private final MutableClock clock = new MutableClock(Instant.parse("2026-04-01T10:00:00Z"));

	private final RefundPolicy policy = new RefundPolicy(this.data, 25000);

	private final AuthorizationTokens tokens = new AuthorizationTokens(Duration.ofMinutes(2), this.clock);

	private final RefundService service = new RefundService(this.policy, this.tokens, this.data, this.clock);

	private RefundIntent intent(String accountId, String chargeId, long amountCents) {
		return new RefundIntent("session-a", accountId, chargeId, amountCents);
	}

	@Test
	void recordsExactlyOneRefundWithinPolicy() {
		RefundOutcome outcome = this.service.request(intent("acct_demo_1", "ch_demo_1", 4200));

		assertThat(outcome).isInstanceOf(RefundOutcome.Approved.class);
		assertThat(this.data.ledger().refunds()).singleElement().satisfies((refund) -> {
			assertThat(refund.chargeId()).isEqualTo("ch_demo_1");
			assertThat(refund.amountCents()).isEqualTo(4200);
		});
		assertThat(this.tokens.outstanding()).isZero();
	}

	@Test
	void refusesAboveCapWithoutIssuingOrLeakingAnything() {
		RefundOutcome outcome = this.service.request(intent("acct_demo_2", "ch_demo_over_cap", 30000));

		assertThat(outcome).isEqualTo(new RefundOutcome.Denied(
				DenialReason.ABOVE_AUTO_APPROVAL_CAP, DenialReason.ABOVE_AUTO_APPROVAL_CAP.message(), true));
		assertThat(((RefundOutcome.Denied) outcome).message()).doesNotContain("30000", "25000");
		assertThat(this.data.ledger().refunds()).isEmpty();
		assertThat(this.tokens.outstanding()).isZero();
	}

	@Test
	void aForgedOrMissingTokenCannotMoveMoney() {
		assertThat(this.service.executeAuthorized("auth_forged", "session-a", "ch_demo_1", 4200))
			.isEqualTo(RefundOutcome.Denied.of(DenialReason.AUTHORIZATION_INVALID));
		assertThat(this.service.executeAuthorized(null, "session-a", "ch_demo_1", 4200))
			.isEqualTo(RefundOutcome.Denied.of(DenialReason.AUTHORIZATION_INVALID));
		assertThat(this.data.ledger().refunds()).isEmpty();
	}

	@Test
	void anAmountMismatchIsRejectedAndTheTokenStaysUnused() {
		String token = this.tokens.mint("session-a", "ch_demo_1", 4200).token();

		RefundOutcome mismatched = this.service.executeAuthorized(token, "session-a", "ch_demo_1", 100);

		assertThat(mismatched).isEqualTo(RefundOutcome.Denied.of(DenialReason.AUTHORIZATION_INVALID));
		assertThat(this.data.ledger().refunds()).isEmpty();
		assertThat(this.tokens.outstanding()).isEqualTo(1);
		assertThat(this.service.executeAuthorized(token, "session-a", "ch_demo_1", 4200))
			.isInstanceOf(RefundOutcome.Approved.class);
	}

	@Test
	void anExpiredTokenIsRejected() {
		String token = this.tokens.mint("session-a", "ch_demo_1", 4200).token();
		this.clock.advance(Duration.ofMinutes(2));

		assertThat(this.service.executeAuthorized(token, "session-a", "ch_demo_1", 4200))
			.isEqualTo(RefundOutcome.Denied.of(DenialReason.AUTHORIZATION_INVALID));
		assertThat(this.data.ledger().refunds()).isEmpty();
	}

	@Test
	void aSecondRefundOnTheSameChargeIsRejected() {
		assertThat(this.service.request(intent("acct_demo_1", "ch_demo_1", 4200)))
			.isInstanceOf(RefundOutcome.Approved.class);

		assertThat(this.service.request(intent("acct_demo_1", "ch_demo_1", 1)))
			.isEqualTo(RefundOutcome.Denied.of(DenialReason.ALREADY_REFUNDED));
		assertThat(this.data.ledger().refunds()).hasSize(1);
	}

	@Test
	void stateChangesBetweenMintingAndConsumingAreCaught() {
		String token = this.tokens.mint("session-a", "ch_demo_1", 4200).token();
		this.data.ledger().record(new Refund("rf_external", "ch_demo_1", 4200, Instant.now(this.clock)));

		assertThat(this.service.executeAuthorized(token, "session-a", "ch_demo_1", 4200))
			.isEqualTo(RefundOutcome.Denied.of(DenialReason.ALREADY_REFUNDED));
		assertThat(this.data.ledger().refunds()).hasSize(1);
	}

	@Test
	void aModelClaimOfApprovalDoesNotWidenAuthority() {
		// The service has no parameter for an approval claim: only policy state counts.
		RefundOutcome outcome = this.service.request(intent("acct_demo_2", "ch_demo_over_cap", 50000));

		assertThat(outcome).isInstanceOf(RefundOutcome.Denied.class);
		assertThat(((RefundOutcome.Denied) outcome).reason())
			.isEqualTo(DenialReason.ABOVE_AUTO_APPROVAL_CAP);
		assertThat(this.data.ledger().refunds()).isEmpty();
	}

	@Test
	void partialRefundsConsumeTheRemainingBalance() {
		assertThat(this.service.request(intent("acct_demo_3", "ch_demo_3", 1000)))
			.isInstanceOf(RefundOutcome.Approved.class);
		assertThat(this.service.request(intent("acct_demo_3", "ch_demo_3", 1500)))
			.isInstanceOf(RefundOutcome.Approved.class);
		assertThat(this.service.request(intent("acct_demo_3", "ch_demo_3", 1)))
			.isEqualTo(RefundOutcome.Denied.of(DenialReason.ALREADY_REFUNDED));

		assertThat(this.data.ledger().refunds()).hasSize(2);
		assertThat(this.data.ledger().refundedAmountCents("ch_demo_3")).isEqualTo(2500);
	}

}
