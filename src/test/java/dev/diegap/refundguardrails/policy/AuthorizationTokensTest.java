package dev.diegap.refundguardrails.policy;

import java.time.Duration;
import java.time.Instant;

import dev.diegap.refundguardrails.testsupport.MutableClock;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationTokensTest {

	private final MutableClock clock = new MutableClock(Instant.parse("2026-04-01T10:00:00Z"));

	private final AuthorizationTokens tokens = new AuthorizationTokens(Duration.ofMinutes(2), this.clock);

	@Test
	void mintsATokenBoundToSessionChargeAndAmount() {
		AuthorizationGrant grant = this.tokens.mint("session-a", "ch_demo_1", 4200);

		assertThat(grant.token()).startsWith("auth_");
		assertThat(grant.sessionId()).isEqualTo("session-a");
		assertThat(grant.chargeId()).isEqualTo("ch_demo_1");
		assertThat(grant.amountCents()).isEqualTo(4200);
		assertThat(grant.expiresAt()).isEqualTo(Instant.parse("2026-04-01T10:02:00Z"));
		assertThat(this.tokens.outstanding()).isEqualTo(1);
	}

	@Test
	void tokensAreUnguessableAndDistinct() {
		String first = this.tokens.mint("session-a", "ch_demo_1", 4200).token();
		String second = this.tokens.mint("session-a", "ch_demo_1", 4200).token();

		assertThat(first).isNotEqualTo(second);
		assertThat(first.length()).isGreaterThanOrEqualTo(24);
	}

	@Test
	void consumesATokenExactlyOnce() {
		String token = this.tokens.mint("session-a", "ch_demo_1", 4200).token();

		assertThat(this.tokens.consume(token, "session-a", "ch_demo_1", 4200))
			.isEqualTo(AuthorizationTokens.Consumption.CONSUMED);
		assertThat(this.tokens.consume(token, "session-a", "ch_demo_1", 4200))
			.isEqualTo(AuthorizationTokens.Consumption.UNKNOWN);
		assertThat(this.tokens.outstanding()).isZero();
	}

	@Test
	void aMismatchedAmountLeavesTheTokenUnused() {
		String token = this.tokens.mint("session-a", "ch_demo_1", 4200).token();

		assertThat(this.tokens.consume(token, "session-a", "ch_demo_1", 100))
			.isEqualTo(AuthorizationTokens.Consumption.MISMATCH);
		assertThat(this.tokens.outstanding()).isEqualTo(1);
		assertThat(this.tokens.consume(token, "session-a", "ch_demo_1", 4200))
			.isEqualTo(AuthorizationTokens.Consumption.CONSUMED);
	}

	@Test
	void aMismatchedSessionOrChargeIsRejected() {
		String token = this.tokens.mint("session-a", "ch_demo_1", 4200).token();

		assertThat(this.tokens.consume(token, "session-b", "ch_demo_1", 4200))
			.isEqualTo(AuthorizationTokens.Consumption.MISMATCH);
		assertThat(this.tokens.consume(token, "session-a", "ch_demo_0", 4200))
			.isEqualTo(AuthorizationTokens.Consumption.MISMATCH);
		assertThat(this.tokens.outstanding()).isEqualTo(1);
	}

	@Test
	void anExpiredTokenIsRejectedAndDiscarded() {
		String token = this.tokens.mint("session-a", "ch_demo_1", 4200).token();

		this.clock.advance(Duration.ofMinutes(2));

		assertThat(this.tokens.consume(token, "session-a", "ch_demo_1", 4200))
			.isEqualTo(AuthorizationTokens.Consumption.EXPIRED);
		assertThat(this.tokens.outstanding()).isZero();
	}

	@Test
	void unknownOrBlankTokensAreRejected() {
		assertThat(this.tokens.consume("auth_forged", "session-a", "ch_demo_1", 4200))
			.isEqualTo(AuthorizationTokens.Consumption.UNKNOWN);
		assertThat(this.tokens.consume(null, "session-a", "ch_demo_1", 4200))
			.isEqualTo(AuthorizationTokens.Consumption.UNKNOWN);
		assertThat(this.tokens.consume("  ", "session-a", "ch_demo_1", 4200))
			.isEqualTo(AuthorizationTokens.Consumption.UNKNOWN);
	}

}
