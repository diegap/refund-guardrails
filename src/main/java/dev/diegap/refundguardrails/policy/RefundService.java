package dev.diegap.refundguardrails.policy;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import dev.diegap.refundguardrails.domain.Charge;
import dev.diegap.refundguardrails.domain.DemoData;
import dev.diegap.refundguardrails.domain.Refund;

/**
 * The only component that moves money. It evaluates policy, mints a single-use
 * authorization, and records the refund only after the token has been consumed.
 * The model never sees or supplies a token; it can only ask.
 */
public class RefundService {

	private final RefundPolicy policy;

	private final AuthorizationTokens tokens;

	private final DemoData data;

	private final Clock clock;

	private final AtomicInteger sequence = new AtomicInteger();

	public RefundService(RefundPolicy policy, AuthorizationTokens tokens, DemoData data, Clock clock) {
		this.policy = policy;
		this.tokens = tokens;
		this.data = data;
		this.clock = clock;
	}

	/** Model-facing path: ask, and let policy decide. */
	public synchronized RefundOutcome request(RefundIntent intent) {
		PolicyDecision decision = this.policy.evaluate(intent);
		if (decision instanceof PolicyDecision.Denied denied) {
			return RefundOutcome.Denied.of(denied.reason());
		}
		AuthorizationGrant grant = this.tokens.mint(intent.sessionId(), intent.chargeId(), intent.amountCents());
		return executeAuthorized(grant.token(), intent.sessionId(), intent.chargeId(), intent.amountCents());
	}

	/**
	 * Enforcement path. A refund is recorded only when the token matches the
	 * session, charge, and amount and policy still allows it.
	 */
	public synchronized RefundOutcome executeAuthorized(String token, String sessionId, String chargeId,
			long amountCents) {
		AuthorizationTokens.Consumption consumption = this.tokens.consume(token, sessionId, chargeId, amountCents);
		if (consumption != AuthorizationTokens.Consumption.CONSUMED) {
			return RefundOutcome.Denied.of(DenialReason.AUTHORIZATION_INVALID);
		}

		Optional<Charge> charge = this.data.charge(chargeId);
		if (charge.isEmpty()) {
			return RefundOutcome.Denied.of(DenialReason.CHARGE_NOT_FOUND);
		}

		// Re-check against current state: the ledger may have changed between the
		// policy decision and this point.
		RefundIntent current = new RefundIntent(sessionId, charge.get().accountId(), chargeId, amountCents);
		PolicyDecision decision = this.policy.evaluate(current);
		if (decision instanceof PolicyDecision.Denied denied) {
			return RefundOutcome.Denied.of(denied.reason());
		}

		Refund refund = new Refund("rf_demo_" + this.sequence.incrementAndGet(), chargeId, amountCents,
				Instant.now(this.clock));
		this.data.ledger().record(refund);
		return new RefundOutcome.Approved(refund.id(), chargeId, amountCents);
	}

}
