package dev.diegap.refundguardrails.evals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EvalCaseLoaderTest {

	@TempDir
	Path tempDir;

	@Test
	void loadsTheGoldenSetFromTheClasspath() {
		EvalSuite suite = EvalCaseLoader.loadClasspath("evals/cases.yaml");

		assertThat(suite.suite()).isEqualTo("refund-guardrails");
		assertThat(suite.scope()).contains("does not measure model quality");
		assertThat(suite.cases()).hasSize(15);
		assertThat(suite.cases()).extracting(EvalCase::name)
			.contains("duplicate-charge-refund", "over-cap-refund", "policy-override-guard-miss",
					"off-topic-refused", "double-refund-prevented");
	}

	@Test
	void addingACaseIsDataOnly() throws Exception {
		String yaml = """
				suite: custom
				scope: This custom suite also does not measure model quality.
				cases:
				  - name: new-case
				    message: Please help with my card 4242 4242 4242 4242.
				    expect:
				      contains: ["help"]
				      ledgerUnchanged: true
				""";
		Path file = this.tempDir.resolve("custom.yaml");
		Files.writeString(file, yaml);

		EvalSuite suite = EvalCaseLoader.load(file);

		assertThat(suite.cases()).singleElement().satisfies((evalCase) -> {
			assertThat(evalCase.name()).isEqualTo("new-case");
			assertThat(evalCase.message()).contains("4242 4242 4242 4242");
			assertThat(evalCase.expect().contains()).containsExactly("help");
			assertThat(evalCase.expect().ledgerUnchanged()).isTrue();
		});
	}

	@Test
	void doesNotAllowACaseToDisableThePrivacyInvariant() throws Exception {
		String yaml = """
				suite: custom
				scope: scope
				cases:
				  - name: leak
				    message: hello
				    expect:
				      noPanInModelBound: false
				""";
		Path file = this.tempDir.resolve("leak.yaml");
		Files.writeString(file, yaml);

		assertThatThrownBy(() -> EvalCaseLoader.load(file)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("cannot disable the privacy invariant");
	}

	@Test
	void rejectsDuplicateCaseNames() throws Exception {
		String yaml = """
				suite: custom
				scope: scope
				cases:
				  - name: same
				    message: hello
				  - name: same
				    message: hello again
				""";
		Path file = this.tempDir.resolve("duplicate.yaml");
		Files.writeString(file, yaml);

		assertThatThrownBy(() -> EvalCaseLoader.load(file)).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining("duplicate evaluation case name");
	}

}
