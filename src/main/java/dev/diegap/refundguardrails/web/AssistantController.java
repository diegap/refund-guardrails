package dev.diegap.refundguardrails.web;

import dev.diegap.refundguardrails.assistant.AssistantTurn;
import dev.diegap.refundguardrails.assistant.AssistantTurnRequest;
import dev.diegap.refundguardrails.assistant.port.in.RefundAssistant;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point for the demo interface and integration tests. */
@RestController
@RequestMapping("/api")
class AssistantController {

	private final RefundAssistant service;

	AssistantController(RefundAssistant service) {
		this.service = service;
	}

	@PostMapping("/turns")
	AssistantTurn turn(@RequestBody AssistantTurnRequest request) {
		return this.service.chat(request);
	}

}
