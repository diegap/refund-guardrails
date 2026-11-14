package dev.diegap.refundguardrails.evals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class EvalCliTest {

	@TempDir
	Path tempDir;

	@Test
	void exitsZeroForAnAllPassRunAndNonZeroForAFailingCase() throws Exception {
		int passing = RefundEvalCli.run("evals/passing-single-case.yaml", this.tempDir.resolve("pass"));
		int failing = RefundEvalCli.run("evals/failing-single-case.yaml", this.tempDir.resolve("fail"));

		assertThat(passing).isZero();
		assertThat(failing).isEqualTo(1);

		JsonNode passReport = new JsonMapper().readTree(this.tempDir.resolve("pass/report.json").toFile());
		assertThat(passReport.path("failed").asInt()).isZero();
		assertThat(passReport.path("cases").get(0).path("outcome").asString()).isEqualTo("PASSED");

		JsonNode failReport = new JsonMapper().readTree(this.tempDir.resolve("fail/report.json").toFile());
		assertThat(failReport.path("failed").asInt()).isEqualTo(1);
		assertThat(failReport.path("cases").get(0).path("outcome").asString()).isEqualTo("FAILED");
		assertThat(failReport.path("cases").get(0).path("failedAssertions").size()).isPositive();
	}

}
