package dev.diegap.refundguardrails.scripted;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mock")
class ScriptedProviderApiTest {

	@LocalServerPort
	private int port;

	@Autowired
	private JsonMapper jsonMapper;

	@Autowired
	private ScriptedRequestCapture capture;

	private final HttpClient http = HttpClient.newHttpClient();

	private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

	@BeforeEach
	void setUp() {
		this.capture.clear();
		this.logs.start();
		((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(this.logs);
	}

	@AfterEach
	void tearDown() {
		((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(this.logs);
		this.logs.stop();
	}

	@Test
	void wellFormedRequestGetsWellFormedResponse() throws Exception {
		HttpResponse<String> response = post(request("default", "s1", "Hello"));

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = this.jsonMapper.readTree(response.body());
		assertThat(body.path("object").asString()).isEqualTo("chat.completion");
		assertThat(body.path("choices")).hasSize(1);
		assertThat(body.path("choices").get(0).path("message").path("content").asString()).isNotBlank();
		assertThat(body.path("usage").path("total_tokens").asLong()).isGreaterThanOrEqualTo(0);
	}

	@Test
	void streamingIsRejectedExplicitly() throws Exception {
		String body = """
				{"model":"gpt-4o-mini","stream":true,"messages":[{"role":"user","content":"Hello"}]}
				""";

		HttpResponse<String> response = post(body);

		assertThat(response.statusCode()).isEqualTo(400);
		JsonNode error = this.jsonMapper.readTree(response.body()).path("error");
		assertThat(error.path("code").asString()).isEqualTo("streaming_unsupported");
		assertThat(error.path("message").asString()).contains("streaming");
	}

	@Test
	void toolCallFixtureCarriesValidJsonArguments() throws Exception {
		HttpResponse<String> response = post(request("duplicate-charge", "s1", "Refund the duplicate"));

		JsonNode call = this.jsonMapper.readTree(response.body())
			.path("choices")
			.get(0)
			.path("message")
			.path("tool_calls")
			.get(0);
		assertThat(call.path("function").path("name").asString()).isEqualTo("lookup_account");
		assertThat(this.jsonMapper.readTree(call.path("function").path("arguments").asString()).path("cardToken")
			.asString()).isEqualTo("[[CARD_1]]");
	}

	@Test
	void unknownScenarioFallsBackAndIsRecorded() throws Exception {
		HttpResponse<String> response = post(request("no-such-scenario", "s-fallback", "Hello"));

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(contentOf(response)).isEqualTo(
				"I can help with accounts, charges, refunds, or escalation to a human agent.");
		ScriptedRequestRecord record = this.capture.lastForSession("s-fallback").orElseThrow();
		assertThat(record.fallback()).isTrue();
		assertThat(record.fixtureName()).isEqualTo(ScriptedFixtureCatalog.DEFAULT_SCENARIO);
		assertThat(record.requestedScenario()).isEqualTo("no-such-scenario");
	}

	@Test
	void repeatedRequestsAreIdentical() throws Exception {
		HttpResponse<String> first = post(request("over-cap-refund", "s1", "Refund 500"));
		HttpResponse<String> second = post(request("over-cap-refund", "s1", "Refund 500"));

		assertThat(this.jsonMapper.readTree(first.body()).path("choices").get(0).path("message"))
			.isEqualTo(this.jsonMapper.readTree(second.body()).path("choices").get(0).path("message"));
	}

	@Test
	void captureKeepsTheExactRequestBodyPerSession() throws Exception {
		String body = request("off-topic", "s-capture", "Tell me a joke");

		post(body);

		ScriptedRequestRecord record = this.capture.lastForSession("s-capture").orElseThrow();
		assertThat(record.rawBody()).isEqualTo(body);
		assertThat(record.afterToolCall()).isFalse();
	}

	@Test
	void concurrentSessionsKeepTheirOwnScenario() throws Exception {
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Callable<HttpResponse<String>> offTopic = () -> post(request("off-topic", "s-a", "Weather?"));
			Callable<HttpResponse<String>> duplicate = () -> post(request("duplicate-charge", "s-b", "Refund?"));

			List<Future<HttpResponse<String>>> results = executor.invokeAll(List.of(offTopic, duplicate));

			assertThat(contentOf(results.get(0).get())).contains("I can only help");
			assertThat(contentOf(results.get(1).get())).isEmpty();
			assertThat(this.capture.lastForSession("s-a").orElseThrow().fixtureName()).isEqualTo("off-topic");
			assertThat(this.capture.lastForSession("s-b").orElseThrow().fixtureName()).isEqualTo("duplicate-charge");
		}
		finally {
			executor.shutdownNow();
		}
	}

	@Test
	void requestBodiesAreNotWrittenToLogs() throws Exception {
		post(request("off-topic", "s-log", "log-marker-9f3a"));

		assertThat(this.logs.list).noneMatch((event) -> event.getFormattedMessage().contains("log-marker-9f3a"));
	}

	private HttpResponse<String> post(String body) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/v1/chat/completions"))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		return this.http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private String contentOf(HttpResponse<String> response) throws Exception {
		return this.jsonMapper.readTree(response.body())
			.path("choices")
			.get(0)
			.path("message")
			.path("content")
			.asString("");
	}

	private static String request(String scenario, String session, String userText) {
		return """
				{"model":"gpt-4o-mini","messages":[
				  {"role":"system","content":"You are a refund assistant. [[scenario:%s]][[session:%s]]"},
				  {"role":"user","content":"%s"}
				],"stream":false}
				""".formatted(scenario, session, userText);
	}

}
