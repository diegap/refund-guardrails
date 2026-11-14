package dev.diegap.refundguardrails.evals;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({ "mock", "eval" })
class EvalSuiteTest {

	private static final int PORT = freePort();

	@DynamicPropertySource
	static void serverPort(DynamicPropertyRegistry registry) {
		registry.add("server.port", () -> PORT);
	}

	@Autowired
	private EvalRunner runner;

	@Autowired
	private JsonMapper jsonMapper;

	@TempDir
	Path tempDir;

	@Test
	void goldenSetPassesAndWritesBothReports() throws Exception {
		EvalSuite suite = EvalCaseLoader.loadClasspath("evals/cases.yaml");

		EvalReport report = this.runner.runAndWrite(suite, this.tempDir);

		assertThat(report.total()).isEqualTo(15);
		assertThat(report.failed()).isZero();
		assertThat(report.passed()).isEqualTo(15);
		assertThat(report.scope()).contains("does not measure model quality");

		Path jsonPath = this.tempDir.resolve("report.json");
		Path markdownPath = this.tempDir.resolve("report.md");
		assertThat(jsonPath).exists();
		assertThat(markdownPath).exists();

		JsonNode json = this.jsonMapper.readTree(jsonPath.toFile());
		assertThat(json.path("suite").asString()).isEqualTo("refund-guardrails");
		assertThat(json.path("total").asInt()).isEqualTo(15);
		assertThat(json.path("passed").asInt()).isEqualTo(15);
		assertThat(json.path("failed").asInt()).isZero();
		assertThat(json.path("scope").asString()).contains("does not measure model quality");
		JsonNode first = json.path("cases").get(0);
		assertThat(first.path("name").asString()).isNotBlank();
		assertThat(first.path("outcome").asString()).isEqualTo("PASSED");
		assertThat(first.path("failedAssertions").isArray()).isTrue();

		String markdown = Files.readString(markdownPath);
		assertThat(markdown).contains("does not measure model quality", "15 cases, 15 passed, 0 failed",
				"| Case | Outcome | Failed assertions |");
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
