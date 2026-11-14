package dev.diegap.refundguardrails.assistant;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import dev.diegap.refundguardrails.security.OutputRedactor;
import org.springframework.stereotype.Component;

/**
 * In-memory escalation tickets. The reason is output-scanned before it is
 * stored, so a model that echoes sensitive text cannot park it here either.
 */
@Component
public class EscalationQueue {

	private final AtomicInteger sequence = new AtomicInteger();

	private final List<Escalation> tickets = new CopyOnWriteArrayList<>();

	private final OutputRedactor redactor;

	public EscalationQueue(OutputRedactor redactor) {
		this.redactor = redactor;
	}

	public EscalationResult create(String sessionId, String reason) {
		String safeReason = this.redactor.redact(reason == null ? "" : reason).text();
		String ticketId = "esc_demo_" + this.sequence.incrementAndGet();
		this.tickets.add(new Escalation(ticketId, sessionId, safeReason));
		return new EscalationResult(ticketId, "created", "A human agent will follow up on this session.");
	}

	public List<Escalation> tickets() {
		return List.copyOf(this.tickets);
	}

	public List<Escalation> ticketsFor(String sessionId) {
		return this.tickets.stream().filter((ticket) -> ticket.sessionId().equals(sessionId)).toList();
	}

	/** Clears the in-memory queue; used to isolate tests and eval cases. */
	public void clear() {
		this.tickets.clear();
	}

	public record Escalation(String ticketId, String sessionId, String reason) {
	}

}
