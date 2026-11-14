package dev.diegap.refundguardrails.policy;

import java.time.Clock;

import dev.diegap.refundguardrails.domain.ChargeRepository;
import dev.diegap.refundguardrails.domain.RefundLedger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(RefundProperties.class)
class PolicyConfiguration {

	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	RefundPolicy refundPolicy(ChargeRepository charges, RefundLedger ledger, RefundProperties properties) {
		return new RefundPolicy(charges, ledger, properties);
	}

	@Bean
	AuthorizationTokens authorizationTokens(RefundProperties properties, Clock clock) {
		return new AuthorizationTokens(properties.authorizationTtl(), clock);
	}

	@Bean
	RefundService refundService(RefundPolicy policy, AuthorizationTokens tokens, ChargeRepository charges,
			RefundLedger ledger, Clock clock) {
		return new RefundService(policy, tokens, charges, ledger, clock);
	}

}
