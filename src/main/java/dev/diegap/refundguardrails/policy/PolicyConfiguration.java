package dev.diegap.refundguardrails.policy;

import java.time.Clock;

import dev.diegap.refundguardrails.domain.DemoData;
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
	RefundPolicy refundPolicy(DemoData data, RefundProperties properties) {
		return new RefundPolicy(data, properties);
	}

	@Bean
	AuthorizationTokens authorizationTokens(RefundProperties properties, Clock clock) {
		return new AuthorizationTokens(properties.authorizationTtl(), clock);
	}

	@Bean
	RefundService refundService(RefundPolicy policy, AuthorizationTokens tokens, DemoData data, Clock clock) {
		return new RefundService(policy, tokens, data, clock);
	}

}
