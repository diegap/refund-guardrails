package dev.diegap.refundguardrails.security;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Session-scoped, in-memory mapping between card numbers and tokens. A token is
 * stable for a given card within a session ({@code [[CARD_1]]}, {@code
 * [[CARD_2]]}, ...). Nothing is persisted, so a restart forgets every mapping.
 * This is a demo control, not a production secret store.
 */
@Component
public class PanVault {

	private static final String DEFAULT_SESSION = "default";

	private final Map<String, SessionVault> sessions = new ConcurrentHashMap<>();

	/** Returns the token for this card, creating it on first use in the session. */
	public String tokenFor(String sessionId, String panDigits) {
		return session(sessionId).tokenFor(panDigits);
	}

	public Optional<String> digitsFor(String sessionId, String token) {
		SessionVault vault = this.sessions.get(normalize(sessionId));
		return vault == null ? Optional.empty() : vault.digitsFor(token);
	}

	public Optional<String> lastFour(String sessionId, String token) {
		return digitsFor(sessionId, token)
			.filter((digits) -> digits.length() >= 4)
			.map((digits) -> digits.substring(digits.length() - 4));
	}

	public void discard(String sessionId) {
		this.sessions.remove(normalize(sessionId));
	}

	/** Number of cards vaulted in a session; used by tests and diagnostics. */
	public int size(String sessionId) {
		SessionVault vault = this.sessions.get(normalize(sessionId));
		return vault == null ? 0 : vault.size();
	}

	private SessionVault session(String sessionId) {
		return this.sessions.computeIfAbsent(normalize(sessionId), (ignored) -> new SessionVault());
	}

	private static String normalize(String sessionId) {
		return sessionId == null || sessionId.isBlank() ? DEFAULT_SESSION : sessionId;
	}

	private static final class SessionVault {

		private final Map<String, String> tokenByDigits = new HashMap<>();

		private final Map<String, String> digitsByToken = new HashMap<>();

		private int next = 1;

		synchronized String tokenFor(String panDigits) {
			String existing = this.tokenByDigits.get(panDigits);
			if (existing != null) {
				return existing;
			}
			String token = "[[CARD_" + this.next++ + "]]";
			this.tokenByDigits.put(panDigits, token);
			this.digitsByToken.put(token, panDigits);
			return token;
		}

		synchronized Optional<String> digitsFor(String token) {
			return Optional.ofNullable(token == null ? null : this.digitsByToken.get(token));
		}

		synchronized int size() {
			return this.digitsByToken.size();
		}

	}

}
