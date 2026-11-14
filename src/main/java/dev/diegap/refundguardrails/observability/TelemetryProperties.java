package dev.diegap.refundguardrails.observability;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Telemetry switches. Content export is off by default; when enabled, the
 * filter redacts card numbers and email addresses before the text reaches a
 * span.
 */
@ConfigurationProperties("app.telemetry")
public record TelemetryProperties(Content content) {

	public TelemetryProperties {
		content = content == null ? new Content(false) : content;
	}

	public record Content(boolean enabled) {
	}

}
