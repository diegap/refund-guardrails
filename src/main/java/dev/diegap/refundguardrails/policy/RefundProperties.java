package dev.diegap.refundguardrails.policy;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Server-side refund limits. Nothing here can be changed by a prompt, a model
 * response, or a request body.
 */
@ConfigurationProperties("app.refund")
public record RefundProperties(long autoApprovalCapCents, Duration authorizationTtl) {

	public RefundProperties {
		if (autoApprovalCapCents <= 0) {
			throw new IllegalArgumentException("auto-approval cap must be positive");
		}
		if (authorizationTtl == null || authorizationTtl.isZero() || authorizationTtl.isNegative()) {
			throw new IllegalArgumentException("authorization TTL must be positive");
		}
	}

}
