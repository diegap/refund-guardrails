package dev.diegap.refundguardrails.policy;

import java.time.Instant;

/**
 * A single-use permission to refund one charge, for one amount, in one session.
 * The token value is unguessable and is never exposed to the model.
 */
public record AuthorizationGrant(String token, String sessionId, String chargeId, long amountCents,
		Instant expiresAt) {

}
