package dev.diegap.refundguardrails.observability;

import java.io.IOException;
import java.net.ServerSocket;

import dev.diegap.refundguardrails.assistant.AssistantTurnRequest;
import dev.diegap.refundguardrails.assistant.RefundAssistantService;
import dev.diegap.refundguardrails.domain.DemoData;
import io.micrometer.common.KeyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 8.3: when content export is enabled, the prompt attribute carries the
 * token and the redactor removes any raw card number.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("mock")
@Import(TelemetryObservationTest.RecordingConfig.class)
@TestPropertySource(properties = "app.telemetry.content.enabled=true")
class TelemetryContentEnabledTest {

	private static final int PORT = freePort();

	private static final String VISA = "4242424242424242";

	private static final String VISA_FORMATTED = "4242 4242 4242 4242";

	@DynamicPropertySource
	static void serverPort(DynamicPropertyRegistry registry) {
		registry.add("server.port", () -> PORT);
	}

	@Autowired
	private RefundAssistantService service;

	@Autowired
	private DemoData data;

	@Autowired
	private TelemetryObservationTest.RecordingObservationHandler handler;

	@BeforeEach
	void reset() {
		this.handler.clear();
		this.data.ledger().clear();
	}

	@Test
	void enabledContentIsRedactedAndShowsTheToken() {
		this.service.chat(new AssistantTurnRequest("telemetry-content",
				"I was charged twice for my Pro plan. Please refund the duplicate. My card is " + VISA_FORMATTED
						+ ".",
				"duplicate-charge"));

		ChatModelObservationContext modelSpan = this.handler.contexts()
			.stream()
			.filter(ChatModelObservationContext.class::isInstance)
			.map(ChatModelObservationContext.class::cast)
			.findFirst()
			.orElseThrow();

		KeyValue prompt = modelSpan.getHighCardinalityKeyValue(ChatContentObservationFilter.PROMPT);
		assertThat(prompt).isNotNull();
		assertThat(prompt.getValue()).contains("[[CARD_1]]").doesNotContain(VISA).doesNotContain(VISA_FORMATTED);

		KeyValue completion = modelSpan.getHighCardinalityKeyValue(ChatContentObservationFilter.COMPLETION);
		assertThat(completion).isNotNull();
		assertThat(completion.getValue()).doesNotContain(VISA).doesNotContain(VISA_FORMATTED);
	}

	private static int freePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
		catch (IOException ex) {
			throw new IllegalStateException("cannot allocate a local port", ex);
		}
	}

}
