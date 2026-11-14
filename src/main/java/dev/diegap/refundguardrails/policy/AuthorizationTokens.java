package dev.diegap.refundguardrails.policy;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Issues and consumes single-use authorization tokens. {@link #mint} is
 * package-private on purpose: only the policy path in this package can create
 * authority, so no advisor, tool, or controller can forge it.
 */
public class AuthorizationTokens {

	public enum Consumption {

		CONSUMED, UNKNOWN, EXPIRED, MISMATCH

	}

	private final Map<String, AuthorizationGrant> grants = new ConcurrentHashMap<>();

	private final SecureRandom random = new SecureRandom();

	private final Duration ttl;

	private final Clock clock;

	public AuthorizationTokens(Duration ttl, Clock clock) {
		this.ttl = ttl;
		this.clock = clock;
	}

	AuthorizationGrant mint(String sessionId, String chargeId, long amountCents) {
		byte[] bytes = new byte[24];
		this.random.nextBytes(bytes);
		String token = "auth_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		Instant expiresAt = Instant.now(this.clock).plus(this.ttl);
		AuthorizationGrant grant = new AuthorizationGrant(token, sessionId, chargeId, amountCents, expiresAt);
		this.grants.put(token, grant);
		return grant;
	}

	/**
	 * Consumes a token only when it is known, unexpired, and matches the session,
	 * charge, and amount. A mismatch leaves the token unused; an expired token is
	 * discarded.
	 */
	public Consumption consume(String token, String sessionId, String chargeId, long amountCents) {
		if (token == null || token.isBlank()) {
			return Consumption.UNKNOWN;
		}
		AuthorizationGrant grant = this.grants.get(token);
		if (grant == null) {
			return Consumption.UNKNOWN;
		}
		if (!grant.expiresAt().isAfter(Instant.now(this.clock))) {
			this.grants.remove(token);
			return Consumption.EXPIRED;
		}
		if (!grant.sessionId().equals(sessionId) || !grant.chargeId().equals(chargeId)
				|| grant.amountCents() != amountCents) {
			return Consumption.MISMATCH;
		}
		this.grants.remove(token);
		return Consumption.CONSUMED;
	}

	/** Number of outstanding tokens; used by tests to prove consumption. */
	public int outstanding() {
		return this.grants.size();
	}

	/** Clears outstanding tokens; used to isolate evaluation cases. */
	public void clear() {
		this.grants.clear();
	}

}
