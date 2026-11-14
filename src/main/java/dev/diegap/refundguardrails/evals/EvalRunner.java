package dev.diegap.refundguardrails.evals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import dev.diegap.refundguardrails.assistant.AssistantTurn;
import dev.diegap.refundguardrails.assistant.AssistantTurnRequest;
import dev.diegap.refundguardrails.assistant.EscalationQueue;
import dev.diegap.refundguardrails.assistant.RefundAssistantService;
import dev.diegap.refundguardrails.assistant.SessionAccounts;
import dev.diegap.refundguardrails.domain.DemoData;
import dev.diegap.refundguardrails.policy.AuthorizationTokens;
import dev.diegap.refundguardrails.scripted.ScriptedRequestCapture;
import dev.diegap.refundguardrails.security.PanDetector;
import dev.diegap.refundguardrails.security.PanVault;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * Runs a case file with fresh session, vault, ledger, escalation, authorization,
 * and telemetry state for every case. Cases use distinct sessions, so a refund
 * or token from one case cannot be observed by the next.
 */
@Service
@Profile("eval")
public class EvalRunner {

	private final RefundAssistantService assistant;

	private final DemoData data;

	private final PanVault vault;

	private final SessionAccounts sessions;

	private final EscalationQueue escalations;

	private final ScriptedRequestCapture capture;

	private final AuthorizationTokens tokens;

	private final EvalTelemetryCapture telemetry;

	private final EvalReportWriter writer;

	private final EvalAssertions assertions;

	public EvalRunner(RefundAssistantService assistant, DemoData data, PanVault vault, SessionAccounts sessions,
			EscalationQueue escalations, ScriptedRequestCapture capture, AuthorizationTokens tokens,
			EvalTelemetryCapture telemetry, EvalReportWriter writer, PanDetector detector) {
		this.assistant = assistant;
		this.data = data;
		this.vault = vault;
		this.sessions = sessions;
		this.escalations = escalations;
		this.capture = capture;
		this.tokens = tokens;
		this.telemetry = telemetry;
		this.writer = writer;
		this.assertions = new EvalAssertions(detector);
	}

	public EvalReport runAndWrite(EvalSuite suite, Path outputDirectory) throws IOException {
		EvalReport report = run(suite);
		this.writer.write(report, outputDirectory);
		return report;
	}

	public EvalReport run(EvalSuite suite) {
		List<EvalCaseResult> results = new ArrayList<>();
		int index = 0;
		for (EvalCase evalCase : suite.cases()) {
			results.add(runCase(evalCase, index++));
		}
		int passed = (int) results.stream().filter(EvalCaseResult::passed).count();
		return new EvalReport(suite.suite(), suite.scope(), results.size(), passed, results.size() - passed,
				results);
	}

	private EvalCaseResult runCase(EvalCase evalCase, int index) {
		String sessionId = "eval-" + index + "-" + slug(evalCase.name());
		reset(sessionId);

		AssistantTurn turn = this.assistant.chat(
				new AssistantTurnRequest(sessionId, evalCase.message(), evalCase.scenario()));
		EvalObservation observation = new EvalObservation(turn, this.capture.forSession(sessionId),
				this.telemetry.contexts(), this.data.ledger().refunds().size());
		return this.assertions.evaluate(evalCase, observation);
	}

	private void reset(String sessionId) {
		this.data.ledger().clear();
		this.escalations.clear();
		this.capture.clear();
		this.telemetry.clear();
		this.tokens.clear();
		this.vault.discard(sessionId);
		this.sessions.clear(sessionId);
	}

	private static String slug(String name) {
		String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
		return slug.length() > 40 ? slug.substring(0, 40) : slug;
	}

}
