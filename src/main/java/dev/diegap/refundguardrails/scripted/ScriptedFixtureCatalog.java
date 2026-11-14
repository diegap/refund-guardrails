package dev.diegap.refundguardrails.scripted;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Deterministic fixture routing. Selection is by the scenario name found in the
 * request; unknown names fall back to the default fixture. Once a tool result is
 * present in the conversation, the fixture's follow-up answer is used.
 */
@Component
@Profile("mock")
public class ScriptedFixtureCatalog {

	public static final String DEFAULT_SCENARIO = "default";

	private final Map<String, ScriptedFixture> fixtures;

	ScriptedFixtureCatalog(List<ScriptedFixture> fixtures) {
		Map<String, ScriptedFixture> byName = new LinkedHashMap<>();
		for (ScriptedFixture fixture : fixtures) {
			ScriptedFixture previous = byName.put(fixture.name(), fixture);
			if (previous != null) {
				throw new IllegalStateException("duplicate scripted fixture: " + fixture.name());
			}
		}
		if (!byName.containsKey(DEFAULT_SCENARIO)) {
			throw new IllegalStateException("the scripted provider needs a '" + DEFAULT_SCENARIO + "' fixture");
		}
		this.fixtures = Map.copyOf(byName);
	}

	public Selection select(String requestedScenario, int toolMessageCount) {
		String requested = requestedScenario == null || requestedScenario.isBlank() ? DEFAULT_SCENARIO : requestedScenario;
		boolean fallback = !this.fixtures.containsKey(requested);
		ScriptedFixture fixture = fallback ? this.fixtures.get(DEFAULT_SCENARIO) : this.fixtures.get(requested);
		return new Selection(fixture.name(), fixture.step(toolMessageCount), fallback, fixture.failure());
	}

	public Map<String, ScriptedFixture> fixtures() {
		return this.fixtures;
	}

	public record Selection(String fixtureName, ScriptedResponse response, boolean fallback,
			ScriptedFailure failure) {
	}

}
