package dev.diegap.refundguardrails.assistant;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Binds a session to the account whose card token the assistant looked up. Tools
 * use this instead of accepting an account id from the model, so the model
 * cannot point a refund at somebody else's charge.
 */
@Component
public class SessionAccounts {

	private static final String DEFAULT_SESSION = "default";

	private final Map<String, String> accountBySession = new ConcurrentHashMap<>();

	public void bind(String sessionId, String accountId) {
		this.accountBySession.put(normalize(sessionId), accountId);
	}

	public Optional<String> accountId(String sessionId) {
		return Optional.ofNullable(this.accountBySession.get(normalize(sessionId)));
	}

	public void clear(String sessionId) {
		this.accountBySession.remove(normalize(sessionId));
	}

	private static String normalize(String sessionId) {
		return sessionId == null || sessionId.isBlank() ? DEFAULT_SESSION : sessionId;
	}

}
