package dev.diegap.refundguardrails.scripted;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("mock")
class ScriptedToolLoopTest {

	private static final int PORT = freePort();

	@DynamicPropertySource
	static void providerProperties(DynamicPropertyRegistry registry) {
		registry.add("server.port", () -> PORT);
	}

	@Autowired
	private ChatClient.Builder chatClientBuilder;

	@Autowired
	private ScriptedRequestCapture capture;

	@BeforeEach
	void clearCapture() {
		this.capture.clear();
	}

	@Test
	void scriptedToolCallDrivesTheLoopToCompletion() {
		AtomicInteger invocations = new AtomicInteger();
		ToolCallback lookupCharges = FunctionToolCallback
			.<LookupChargesRequest, LookupChargesResult>builder("lookup_charges", (request) -> {
				invocations.incrementAndGet();
				return new LookupChargesResult("ch_demo_0", "duplicate of ch_demo_1", 4200);
			})
			.description("Look up charges for an account")
			.inputType(LookupChargesRequest.class)
			.build();

		String reply = this.chatClientBuilder.build()
			.prompt()
			.system("[[scenario:duplicate-charge]][[session:tool-loop]]")
			.user("Please refund my duplicate charge")
			.tools(lookupCharges)
			.call()
			.content();

		assertThat(reply).isEqualTo("I found the duplicate charge and issued a refund within policy.");
		assertThat(invocations).hasValue(1);

		List<ScriptedRequestRecord> requests = this.capture.history();
		assertThat(requests).hasSize(2);
		assertThat(requests.get(0).afterToolCall()).isFalse();
		assertThat(requests.get(1).afterToolCall()).isTrue();
		assertThat(requests.get(1).rawBody()).contains("\"role\":\"tool\"");
		assertThat(requests.get(1).sessionId()).isEqualTo("tool-loop");
	}

	private static int freePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
		catch (IOException ex) {
			throw new IllegalStateException("cannot allocate a local port", ex);
		}
	}

	record LookupChargesRequest(String accountId) {
	}

	record LookupChargesResult(String chargeId, String memo, long amountCents) {
	}

}
