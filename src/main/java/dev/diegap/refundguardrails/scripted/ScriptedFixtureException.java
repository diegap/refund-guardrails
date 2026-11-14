package dev.diegap.refundguardrails.scripted;

/** Raised when a fixture is internally invalid, so the failure names the fixture. */
public class ScriptedFixtureException extends RuntimeException {

	private final String fixtureName;

	ScriptedFixtureException(String fixtureName, String message, Throwable cause) {
		super(message, cause);
		this.fixtureName = fixtureName;
	}

	public String fixtureName() {
		return this.fixtureName;
	}

}
