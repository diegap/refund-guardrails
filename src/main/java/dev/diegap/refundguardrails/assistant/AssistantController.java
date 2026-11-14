package dev.diegap.refundguardrails.assistant;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** HTTP entry point for the demo interface and integration tests. */
@RestController
@RequestMapping("/api")
class AssistantController {

	private final RefundAssistantService service;

	AssistantController(RefundAssistantService service) {
		this.service = service;
	}

	@PostMapping("/turns")
	AssistantTurn turn(@RequestBody AssistantTurnRequest request) {
		return this.service.chat(request);
	}

}
