package dev.diegap.refundguardrails.demo;

import java.util.Optional;
import java.util.UUID;

import dev.diegap.refundguardrails.assistant.AssistantTurn;
import dev.diegap.refundguardrails.assistant.AssistantTurnRequest;
import dev.diegap.refundguardrails.assistant.port.in.RefundAssistant;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** The single-page demo. htmx swaps the fragment; there is no JS build step. */
@Controller
class DemoController {

	private final RefundAssistant service;

	DemoController(RefundAssistant service) {
		this.service = service;
	}

	@GetMapping("/")
	String index(@RequestParam(name = "scenario", defaultValue = "") String scenario, Model model) {
		String sessionId = "demo-" + UUID.randomUUID().toString().substring(0, 8);
		model.addAttribute("sessionId", sessionId);
		model.addAttribute("scenarios", DemoScenario.values());

		Optional<DemoScenario> selected = DemoScenario.fromId(scenario);
		selected.ifPresent((chosen) -> {
			AssistantTurn turn = this.service.chat(new AssistantTurnRequest(sessionId, chosen.message(), chosen.id()));
			model.addAttribute("turn", turn);
			model.addAttribute("scenario", chosen);
		});
		return "index";
	}

	@PostMapping("/demo/turns")
	String turn(@RequestParam("sessionId") String sessionId,
			@RequestParam(name = "message", defaultValue = "") String message,
			@RequestParam(name = "scenario", defaultValue = "") String scenario, Model model) {
		Optional<DemoScenario> selected = DemoScenario.fromId(scenario);
		String effectiveMessage = selected.map(DemoScenario::message).orElse(message);
		AssistantTurn turn = this.service.chat(
				new AssistantTurnRequest(sessionId, effectiveMessage, selected.map(DemoScenario::id).orElse(null)));
		model.addAttribute("turn", turn);
		model.addAttribute("scenario", selected.orElse(null));
		return "fragments/turn :: turn";
	}

}
