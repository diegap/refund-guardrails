package dev.diegap.refundguardrails.assistant.port.in;

import dev.diegap.refundguardrails.assistant.AssistantTurn;
import dev.diegap.refundguardrails.assistant.AssistantTurnRequest;

/** Driving port for one conversational turn. */
public interface RefundAssistant {

	AssistantTurn chat(AssistantTurnRequest request);

}
