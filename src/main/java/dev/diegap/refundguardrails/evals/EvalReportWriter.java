package dev.diegap.refundguardrails.evals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Writes the machine-readable and human-readable evaluation reports. */
@Component
public class EvalReportWriter {

	private final JsonMapper jsonMapper;

	public EvalReportWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public void write(EvalReport report, Path outputDirectory) throws IOException {
		Files.createDirectories(outputDirectory);
		this.jsonMapper.writeValue(outputDirectory.resolve("report.json"), report);
		Files.writeString(outputDirectory.resolve("report.md"), markdown(report));
	}

	private static String markdown(EvalReport report) {
		StringBuilder markdown = new StringBuilder();
		markdown.append("# ").append(report.suite()).append(" evaluation\n\n");
		markdown.append("**Scope:** ").append(report.scope()).append("\n\n");
		markdown.append("**Totals:** ").append(report.total()).append(" cases, ")
			.append(report.passed()).append(" passed, ")
			.append(report.failed()).append(" failed.\n\n");
		markdown.append("| Case | Outcome | Failed assertions |\n");
		markdown.append("| --- | --- | --- |\n");
		for (EvalCaseResult result : report.cases()) {
			String failures = result.failedAssertions().isEmpty() ? "—"
					: result.failedAssertions()
						.stream()
						.map((failure) -> failure.assertion() + ": " + failure.detail())
						.collect(Collectors.joining("<br>"))
						.replace("|", "\\|");
			markdown.append("| ").append(result.name()).append(" | ").append(result.outcome())
				.append(" | ").append(failures).append(" |\n");
		}
		return markdown.toString();
	}

}
