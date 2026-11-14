package dev.diegap.refundguardrails.evals;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("eval")
class EvalConfiguration {

	@Bean
	ObservationRegistryCustomizer<ObservationRegistry> evalTelemetryCustomizer(EvalTelemetryCapture capture) {
		return (registry) -> registry.observationConfig().observationHandler(capture);
	}

}
