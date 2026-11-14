package dev.diegap.refundguardrails.evals;

import java.time.Instant;
import java.util.List;

import dev.diegap.refundguardrails.assistant.AssistantTurn;
import dev.diegap.refundguardrails.assistant.FailureCategory;
import dev.diegap.refundguardrails.assistant.GuardEvaluation;
import dev.diegap.refundguardrails.assistant.PromptInjectionGuardAdvisor;
import dev.diegap.refundguardrails.assistant.TurnMetadata;
import dev.diegap.refundguardrails.scripted.ScriptedRequestRecord;
import dev.diegap.refundguardrails.security.PanDetector;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EvalAssertionsTest {

	private static final String VISA = "4242424242424242";

	private final EvalAssertions assertions = new EvalAssertions(new PanDetector());

	@Test
	void guardFiredPassesOnlyWhenThatGuardFailed() {
		assertThat(evaluate(expectsFired("prompt-injection"), "refused",
				List.of(GuardEvaluation.failed("prompt-injection", "instruction_override_1")), List.of(), "ok", 0)
			.passed()).isTrue();
		assertThat(evaluate(expectsFired("topic-scope"), "refused",
				List.of(GuardEvaluation.failed("prompt-injection", "instruction_override_1")), List.of(), "ok", 0)
			.failedAssertions()).anySatisfy((failure) -> assertThat(failure.assertion()).isEqualTo("guardsFired"));
		assertThat(evaluate(expectsFired("prompt-injection"), "refused", List.of(), List.of(), "ok", 0)
			.failedAssertions()).anySatisfy(
					(failure) -> assertThat(failure.detail()).contains("was not evaluated"));
	}

	@Test
	void guardPassedPassesOnlyWhenThatGuardPassed() {
		assertThat(evaluate(expectsPassed("card-number-redaction"), "ok",
				List.of(GuardEvaluation.passed("card-number-redaction")), List.of(), "ok", 0).passed()).isTrue();
		assertThat(evaluate(expectsPassed("card-number-redaction"), "ok", List.of(), List.of(), "ok", 0)
			.failedAssertions()).anySatisfy((failure) -> assertThat(failure.assertion()).isEqualTo("guardsPassed"));
	}

	@Test
	void refusalPassesOnlyForAKnownRefusal() {
		assertThat(evaluate(expectsRefusal(), PromptInjectionGuardAdvisor.REFUSAL, List.of(), List.of(), "ok", 0)
			.passed()).isTrue();
		assertThat(evaluate(expectsRefusal(), "I issued a refund.", List.of(), List.of(), "ok", 0)
			.failedAssertions()).singleElement()
			.satisfies((failure) -> assertThat(failure.assertion()).isEqualTo("refusal"));
	}

	@Test
	void toolSequencePassesOnlyOnAnExactOrderedMatch() {
		assertThat(evaluate(expectsTools(List.of("lookup_account", "issue_refund")), "ok", List.of(),
				List.of("lookup_account", "issue_refund"), "ok", 0).passed()).isTrue();
		assertThat(evaluate(expectsTools(List.of("lookup_account", "issue_refund")), "ok", List.of(),
				List.of("issue_refund", "lookup_account"), "ok", 0).failedAssertions()).singleElement()
			.satisfies((failure) -> assertThat(failure.assertion()).isEqualTo("toolSequence"));
		assertThat(evaluate(expectsTools(List.of()), "ok", List.of(), List.of(), "ok", 0).passed()).isTrue();
		assertThat(evaluate(expectsTools(List.of()), "ok", List.of(), List.of("lookup_account"), "ok", 0)
			.failedAssertions()).singleElement();
	}

	@Test
	void containsAndOmitsWorkInBothDirections() {
		assertThat(evaluate(expectsContains("refund"), "I issued a refund.", List.of(), List.of(), "ok", 0).passed())
			.isTrue();
		assertThat(evaluate(expectsContains("refund"), "I issued a credit.", List.of(), List.of(), "ok", 0)
			.failedAssertions()).anySatisfy((failure) -> assertThat(failure.assertion()).isEqualTo("contains"));

		assertThat(evaluate(expectsOmits("Exception"), "All good.", List.of(), List.of(), "ok", 0).passed())
			.isTrue();
		assertThat(evaluate(expectsOmits("Exception"), "An Exception happened.", List.of(), List.of(), "ok", 0)
			.failedAssertions()).anySatisfy((failure) -> assertThat(failure.assertion()).isEqualTo("omits"));
	}

	@Test
	void modelBoundContainsAndNoPanAssertionsWork() {
		assertThat(evaluate(expectsModelBound("[[CARD_1]]", true), "ok", List.of(), List.of(),
				"card [[CARD_1]]", 0).passed()).isTrue();
		assertThat(evaluate(expectsModelBound("[[CARD_2]]", true), "ok", List.of(), List.of(),
				"card [[CARD_1]]", 0).failedAssertions()).anySatisfy(
						(failure) -> assertThat(failure.assertion()).isEqualTo("modelBoundContains"));
		assertThat(evaluate(expectsModelBound(null, true), "ok", List.of(), List.of(), "card " + VISA, 0)
			.failedAssertions()).anySatisfy((failure) -> assertThat(failure.assertion()).isEqualTo("privacy"));
	}

	@Test
	void ledgerAssertionsWork() {
		assertThat(evaluate(expectsLedgerUnchanged(), "ok", List.of(), List.of(), "ok", 0).passed()).isTrue();
		assertThat(evaluate(expectsLedgerUnchanged(), "ok", List.of(), List.of(), "ok", 1).failedAssertions())
			.singleElement()
			.satisfies((failure) -> assertThat(failure.assertion()).isEqualTo("ledgerUnchanged"));

		assertThat(evaluate(expectsLedgerRefunds(1), "ok", List.of(), List.of(), "ok", 1).passed()).isTrue();
		assertThat(evaluate(expectsLedgerRefunds(1), "ok", List.of(), List.of(), "ok", 0).failedAssertions())
			.singleElement();
	}

	@Test
	void failureCategoryIsCheckedWhenDeclared() {
		assertThat(evaluateWithCategory(expectsFailure("TOOL_ERROR"), FailureCategory.TOOL_ERROR).passed()).isTrue();
		assertThat(evaluateWithCategory(expectsFailure("NONE"), FailureCategory.TOOL_ERROR).failedAssertions())
			.singleElement()
			.satisfies((failure) -> assertThat(failure.assertion()).isEqualTo("failureCategory"));
	}

	@Test
	void thePrivacyInvariantFailsAnOtherwisePassingCase() {
		EvalCaseResult result = evaluate(expectsContains("ok"), "ok", List.of(), List.of(),
				"the model saw " + VISA, 0);

		assertThat(result.passed()).isFalse();
		assertThat(result.failedAssertions()).anySatisfy(
				(failure) -> assertThat(failure.assertion()).isEqualTo("privacy"));
	}

	@Test
	void thePrivacyInvariantChecksTelemetry() {
		Observation.Context context = new Observation.Context();
		context.addHighCardinalityKeyValue(KeyValue.of("gen_ai.prompt", "card " + VISA));

		EvalCaseResult result = evaluateWithTelemetry(expectsContains("ok"), "ok",
				"clean model-bound content", List.of(context));

		assertThat(result.passed()).isFalse();
		assertThat(result.failedAssertions()).anySatisfy((failure) -> {
			assertThat(failure.assertion()).isEqualTo("privacy");
			assertThat(failure.detail()).contains("telemetry");
			assertThat(failure.detail()).doesNotContain(VISA);
		});
	}

	@Test
	void thePrivacyInvariantChecksScriptedRequests() {
		ScriptedRequestRecord request = new ScriptedRequestRecord("s", null, "fixture", false, 0,
				"{\"content\":\"card " + VISA + "\"}", Instant.EPOCH);

		EvalCaseResult result = evaluateWithRequests(expectsContains("ok"), "ok", "clean", List.of(request));

		assertThat(result.passed()).isFalse();
		assertThat(result.failedAssertions()).anySatisfy(
				(failure) -> assertThat(failure.detail()).contains("scriptedRequest"));
	}

	@Test
	void thePrivacyInvariantChecksTheReply() {
		EvalCaseResult result = evaluate(expectsContains("card"), "the card is " + VISA, List.of(), List.of(), "ok", 0);

		assertThat(result.passed()).isFalse();
		assertThat(result.failedAssertions()).anySatisfy(
				(failure) -> assertThat(failure.detail()).contains("reply"));
	}

	@Test
	void aCleanCasePasses() {
		EvalCaseResult result = evaluate(expectsContains("refund"), "I issued a refund.", List.of(),
				List.of("issue_refund"), "card [[CARD_1]]", 1);

		assertThat(result.passed()).isTrue();
		assertThat(result.outcome()).isEqualTo("PASSED");
	}

	private EvalCaseResult evaluate(EvalExpectations expect, String reply, List<GuardEvaluation> guards,
			List<String> tools, String modelBound, int refunds) {
		return evaluateWith(expect, reply, guards, tools, modelBound, refunds, FailureCategory.NONE, List.of(),
				List.of());
	}

	private EvalCaseResult evaluateWithCategory(EvalExpectations expect, FailureCategory category) {
		return evaluateWith(expect, "ok", List.of(), List.of(), "ok", 0, category, List.of(), List.of());
	}

	private EvalCaseResult evaluateWithTelemetry(EvalExpectations expect, String reply, String modelBound,
			List<Observation.Context> telemetry) {
		return evaluateWith(expect, reply, List.of(), List.of(), modelBound, 0, FailureCategory.NONE, List.of(),
				telemetry);
	}

	private EvalCaseResult evaluateWithRequests(EvalExpectations expect, String reply, String modelBound,
			List<ScriptedRequestRecord> requests) {
		return evaluateWith(expect, reply, List.of(), List.of(), modelBound, 0, FailureCategory.NONE, requests,
				List.of());
	}

	private EvalCaseResult evaluateWith(EvalExpectations expect, String reply, List<GuardEvaluation> guards,
			List<String> tools, String modelBound, int refunds, FailureCategory category,
			List<ScriptedRequestRecord> requests, List<Observation.Context> telemetry) {
		TurnMetadata metadata = new TurnMetadata("raw input", modelBound, guards, tools, List.of(), category);
		AssistantTurn turn = new AssistantTurn("session", reply, metadata);
		EvalObservation observation = new EvalObservation(turn, requests, telemetry, refunds);
		return this.assertions.evaluate(new EvalCase("case", "message", null, expect), observation);
	}

	private static EvalExpectations expectsFired(String... names) {
		return expectations(List.of(names), null, null, null, null, null, null, null, null, null, null);
	}

	private static EvalExpectations expectsPassed(String... names) {
		return expectations(null, List.of(names), null, null, null, null, null, null, null, null, null);
	}

	private static EvalExpectations expectsRefusal() {
		return expectations(null, null, true, null, null, null, null, null, null, null, null);
	}

	private static EvalExpectations expectsTools(List<String> tools) {
		return expectations(null, null, null, tools, null, null, null, null, null, null, null);
	}

	private static EvalExpectations expectsContains(String text) {
		return expectations(null, null, null, null, List.of(text), null, null, null, null, null, null);
	}

	private static EvalExpectations expectsOmits(String text) {
		return expectations(null, null, null, null, null, List.of(text), null, null, null, null, null);
	}

	private static EvalExpectations expectsModelBound(String text, boolean noPan) {
		return expectations(null, null, null, null, null, null, noPan, text == null ? null : List.of(text),
				null, null, null);
	}

	private static EvalExpectations expectsLedgerUnchanged() {
		return expectations(null, null, null, null, null, null, null, null, null, true, null);
	}

	private static EvalExpectations expectsLedgerRefunds(int count) {
		return expectations(null, null, null, null, null, null, null, null, count, null, null);
	}

	private static EvalExpectations expectsFailure(String category) {
		return expectations(null, null, null, null, null, null, null, null, null, null, category);
	}

	private static EvalExpectations expectations(List<String> fired, List<String> passed, Boolean refusal,
			List<String> tools, List<String> contains, List<String> omits, Boolean noPan,
			List<String> modelBoundContains, Integer ledgerRefunds, Boolean ledgerUnchanged, String category) {
		return new EvalExpectations(fired, passed, refusal, tools, contains, omits, noPan, modelBoundContains,
				ledgerRefunds, ledgerUnchanged, category);
	}

}
