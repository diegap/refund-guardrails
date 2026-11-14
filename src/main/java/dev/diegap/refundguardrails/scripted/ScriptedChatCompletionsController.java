package dev.diegap.refundguardrails.scripted;

import java.time.Instant;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Spike 2.2: the smallest possible OpenAI-compatible chat-completions endpoint.
 * It exists so the real {@code OpenAiChatModel} HTTP path runs, which is what
 * produces Spring AI's model-call observation. Group 3 replaces this with a
 * fixture-driven provider.
 */
@RestController
@Profile("mock")
class ScriptedChatCompletionsController {

	@PostMapping(path = "/v1/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE)
	Map<String, Object> chatCompletions(@RequestBody Map<String, Object> request) {
		String model = String.valueOf(request.getOrDefault("model", "scripted-model"));
		return Map.of(
				"id", "chatcmpl-spike",
				"object", "chat.completion",
				"created", Instant.now().getEpochSecond(),
				"model", model,
				"choices", new Object[] { Map.of(
						"index", 0,
						"message", Map.of("role", "assistant", "content", "Spike reply: the scripted provider is reachable."),
						"finish_reason", "stop") },
				"usage", Map.of("prompt_tokens", 9, "completion_tokens", 7, "total_tokens", 16));
	}

}
