package dev.diegap.refundguardrails.assistant.port.out;

/** A coarse, framework-free model failure classification. */
public final class ModelFailure extends RuntimeException {

	public enum Kind {

		TOOL, PROVIDER

	}

	private final Kind kind;

	public ModelFailure(Kind kind, Throwable cause) {
		super(cause == null ? null : cause.getMessage(), cause);
		this.kind = kind;
	}

	public Kind kind() {
		return this.kind;
	}

}
