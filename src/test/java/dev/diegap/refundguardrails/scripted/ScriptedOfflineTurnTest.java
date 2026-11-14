package dev.diegap.refundguardrails.scripted;

import java.io.IOException;
import java.net.ServerSocket;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 3.5: a turn completes with no real key and no outbound traffic. The
 * only endpoint configured is the in-process scripted provider on loopback.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("mock")
class ScriptedOfflineTurnTest {

	private static final int PORT = freePort();

	@DynamicPropertySource
	static void providerProperties(DynamicPropertyRegistry registry) {
		registry.add("server.port", () -> PORT);
		registry.add("spring.ai.openai.api-key", () -> "offline-dummy-key");
	}

	@Autowired
	private ChatClient.Builder chatClientBuilder;

	@Autowired
	private Environment environment;

	@Test
	void turnCompletesAgainstTheLoopbackProvider() {
		String reply = this.chatClientBuilder.build()
			.prompt()
			.system("[[scenario:off-topic]][[session:offline]]")
			.user("Can I get a refund?")
			.call()
			.content();

		assertThat(reply).contains("I can only help with accounts, charges, refunds, or escalation");
		assertThat(this.environment.getProperty("spring.ai.openai.base-url"))
			.isEqualTo("http://localhost:" + PORT + "/v1");
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
