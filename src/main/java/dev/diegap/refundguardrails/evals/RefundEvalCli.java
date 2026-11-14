package dev.diegap.refundguardrails.evals;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Path;
import java.util.Map;

import dev.diegap.refundguardrails.RefundGuardrailsApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Headless entry point: starts the app on a free loopback port, runs the case
 * file, writes both reports under {@code build/evals}, and exits non-zero if any
 * case failed.
 */
public final class RefundEvalCli {

	private RefundEvalCli() {
	}

	public static void main(String[] args) {
		System.exit(run("evals/cases.yaml", Path.of("build", "evals")));
	}

	static int run(String casesResource, Path outputDirectory) {
		int port = freePort();
		SpringApplication application = new SpringApplication(RefundGuardrailsApplication.class);
		application.setAdditionalProfiles("mock", "eval");
		application.setDefaultProperties(Map.of("server.port", String.valueOf(port)));

		int exitCode;
		try (ConfigurableApplicationContext context = application.run()) {
			EvalSuite suite = EvalCaseLoader.loadClasspath(casesResource);
			EvalRunner runner = context.getBean(EvalRunner.class);
			EvalReport report;
			try {
				report = runner.runAndWrite(suite, outputDirectory);
			}
			catch (IOException ex) {
				throw new IllegalStateException("cannot write evaluation reports", ex);
			}

			System.out.println("Evaluation: " + report.passed() + "/" + report.total() + " passed, "
					+ report.failed() + " failed");
			for (EvalCaseResult result : report.cases()) {
				if (!result.passed()) {
					System.out.println("FAILED " + result.name());
					for (AssertionFailure failure : result.failedAssertions()) {
						System.out.println("  - " + failure.assertion() + ": " + failure.detail());
					}
				}
			}
			exitCode = report.failed() == 0 ? 0 : 1;
		}
		return exitCode;
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
