package dev.diegap.refundguardrails.scripted;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * In-memory capture of model-bound requests, keyed by session. Nothing here
 * touches the file system or the logger.
 */
@Component
@Profile("mock")
public class ScriptedRequestCapture {

	private final Map<String, ScriptedRequestRecord> lastBySession = new ConcurrentHashMap<>();

	private final List<ScriptedRequestRecord> history = new CopyOnWriteArrayList<>();

	void record(ScriptedRequestRecord request) {
		this.lastBySession.put(request.sessionId(), request);
		this.history.add(request);
	}

	public Optional<ScriptedRequestRecord> lastForSession(String sessionId) {
		return Optional.ofNullable(this.lastBySession.get(sessionId));
	}

	public List<ScriptedRequestRecord> forSession(String sessionId) {
		return this.history.stream().filter((request) -> request.sessionId().equals(sessionId)).toList();
	}

	public List<ScriptedRequestRecord> history() {
		return List.copyOf(this.history);
	}

	public void clear() {
		this.lastBySession.clear();
		this.history.clear();
	}

}
