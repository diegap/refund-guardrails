package dev.diegap.refundguardrails.scripted;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dev.diegap.refundguardrails.scripted.ScriptedFixtureCatalog.Selection;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * OpenAI-compatible, non-streaming chat-completions endpoint used in the
 * {@code mock} profile. It exists so the real provider client path (including
 * its observation spans) runs without a key or a network.
 */
@RestController
@Profile("mock")
class ScriptedChatCompletionsController {

	static final String DEFAULT_SESSION = "default";

	private static final Pattern SCENARIO_MARKER = Pattern.compile("\\[\\[scenario:([A-Za-z0-9._-]+)]]");

	private static final Pattern SESSION_MARKER = Pattern.compile("\\[\\[session:([A-Za-z0-9._-]+)]]");

	private final JsonMapper jsonMapper;

	private final ScriptedFixtureCatalog catalog;

	private final ScriptedRequestCapture capture;

	ScriptedChatCompletionsController(JsonMapper jsonMapper, ScriptedFixtureCatalog catalog,
			ScriptedRequestCapture capture) {
		this.jsonMapper = jsonMapper;
		this.catalog = catalog;
		this.capture = capture;
	}

	@PostMapping(path = "/v1/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<Map<String, Object>> chatCompletions(@RequestBody String rawBody) {
		JsonNode request;
		try {
			request = this.jsonMapper.readTree(rawBody);
		}
		catch (JacksonException ex) {
			return ResponseEntity.badRequest()
				.body(error("Request body is not valid JSON.", "invalid_request_error", "invalid_json"));
		}

		if (request.path("stream").asBoolean(false)) {
			return ResponseEntity.badRequest()
				.body(error("The scripted provider does not support streaming; send stream=false.",
						"invalid_request_error", "streaming_unsupported"));
		}

		List<JsonNode> messages = messages(request);
		String sessionId = marker(messages, SESSION_MARKER).orElse(DEFAULT_SESSION);
		String requestedScenario = marker(messages, SCENARIO_MARKER).orElse(null);
		int toolMessageCount = (int) messages.stream()
			.filter((message) -> "tool".equals(text(message, "role")))
			.count();

		Selection selection = this.catalog.select(requestedScenario, toolMessageCount);
		this.capture.record(new ScriptedRequestRecord(sessionId, requestedScenario, selection.fixtureName(),
				selection.fallback(), toolMessageCount, rawBody, Instant.now()));

		if (selection.failure() != null) {
			return ResponseEntity.status(selection.failure().status())
				.body(error(selection.failure().message(), "provider_error", selection.failure().code()));
		}

		validateToolArguments(selection);
		return ResponseEntity.ok(payload(request, selection));
	}

	@ExceptionHandler(ScriptedFixtureException.class)
	ResponseEntity<Map<String, Object>> invalidFixture(ScriptedFixtureException ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(error(ex.getMessage(), "server_error", "invalid_fixture"));
	}

	private Map<String, Object> payload(JsonNode request, Selection selection) {
		ScriptedResponse response = selection.response();
		String model = request.path("model").asString("scripted-model");

		Map<String, Object> message = new LinkedHashMap<>();
		message.put("role", "assistant");
		message.put("content", response.content());
		if (response.hasToolCalls()) {
			message.put("tool_calls", response.toolCalls().stream().map(this::toolCallJson).toList());
		}

		Map<String, Object> choice = new LinkedHashMap<>();
		choice.put("index", 0);
		choice.put("message", message);
		choice.put("finish_reason", response.hasToolCalls() ? "tool_calls" : "stop");

		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("id", "chatcmpl-scripted-" + selection.fixtureName() + (selection.fallback() ? "-fallback" : ""));
		payload.put("object", "chat.completion");
		payload.put("created", Instant.now().getEpochSecond());
		payload.put("model", model);
		payload.put("choices", List.of(choice));
		payload.put("usage", usage(request, response));
		return payload;
	}

	private Map<String, Object> toolCallJson(ScriptedToolCall toolCall) {
		return Map.of("id", toolCall.id(), "type", "function",
				"function", Map.of("name", toolCall.name(), "arguments", toolCall.arguments()));
	}

	private Map<String, Object> usage(JsonNode request, ScriptedResponse response) {
		int promptTokens = Math.max(1, request.toString().length() / 4);
		String produced = response.content() != null ? response.content()
				: response.toolCalls().stream().map(ScriptedToolCall::arguments).reduce("", String::concat);
		int completionTokens = Math.max(1, produced.length() / 4);
		return Map.of("prompt_tokens", promptTokens, "completion_tokens", completionTokens, "total_tokens",
				promptTokens + completionTokens);
	}

	private List<JsonNode> messages(JsonNode request) {
		List<JsonNode> messages = new ArrayList<>();
		request.path("messages").forEach(messages::add);
		return messages;
	}

	private Optional<String> marker(List<JsonNode> messages, Pattern pattern) {
		return messages.stream()
			.map(this::messageText)
			.map(pattern::matcher)
			.filter(Matcher::find)
			.map((matcher) -> matcher.group(1))
			.findFirst();
	}

	private String messageText(JsonNode message) {
		JsonNode content = message.path("content");
		if (content.isString()) {
			return content.asString();
		}
		StringBuilder text = new StringBuilder();
		if (content.isArray()) {
			content.forEach((part) -> {
				if (part.isString()) {
					text.append(part.asString()).append('\n');
				}
				else if (part.path("text").isString()) {
					text.append(part.path("text").asString()).append('\n');
				}
			});
		}
		return text.toString();
	}

	private String text(JsonNode message, String field) {
		return message.path(field).asString("");
	}

	private void validateToolArguments(Selection selection) {
		for (ScriptedToolCall toolCall : selection.response().toolCalls()) {
			try {
				this.jsonMapper.readTree(toolCall.arguments());
			}
			catch (JacksonException ex) {
				throw new ScriptedFixtureException(selection.fixtureName(),
						"Fixture '" + selection.fixtureName() + "' returned invalid JSON arguments for tool '"
								+ toolCall.name() + "'.",
						ex);
			}
		}
	}

	private static Map<String, Object> error(String message, String type, String code) {
		return Map.of("error", Map.of("message", message, "type", type, "code", code));
	}

}
