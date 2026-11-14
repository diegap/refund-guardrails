package dev.diegap.refundguardrails.evals;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles({ "mock", "eval" })
class EvalIsolationTest {

	private static final int PORT = freePort();

	@DynamicPropertySource
	static void serverPort(DynamicPropertyRegistry registry) {
		registry.add("server.port", () -> PORT);
	}

	@Autowired
	private EvalRunner runner;

	@Test
	void sequentialCasesCannotSeeEachOthersRefundsOrVaultTokens() {
		EvalCase first = new EvalCase("duplicate-first",
				"I was charged twice for my Pro plan. Please refund the duplicate. "
						+ "My card is 4242 4242 4242 4242.",
				"duplicate-charge",
				new EvalExpectations(null, null, null, null, List.of("issued a refund"), null, null,
						List.of("[[CARD_1]]"), 1, null, null));
		EvalCase second = new EvalCase("team-second",
				"Please refund the Team plan charge. My card is 5555 5555 5555 4444.", "team-plan-refund",
				new EvalExpectations(null, null, null, null, List.of("Team plan"), null, null,
						List.of("[[CARD_1]]"), 1, null, null));
		EvalSuite suite = new EvalSuite("isolation", "Isolation check. It does not measure model quality.",
				List.of(first, second));

		EvalReport report = this.runner.run(suite);

		assertThat(report.total()).isEqualTo(2);
		assertThat(report.failed()).as(report.cases().toString()).isZero();
		assertThat(report.cases()).allSatisfy((result) -> assertThat(result.outcome()).isEqualTo("PASSED"));
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
