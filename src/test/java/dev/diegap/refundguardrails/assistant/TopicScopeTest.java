package dev.diegap.refundguardrails.assistant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TopicScopeTest {

	private final TopicScope scope = new TopicScope();

	@Test
	void acceptsMoneySupportTopics() {
		assertThat(this.scope.isInScope("I was charged twice for my Pro plan.")).isTrue();
		assertThat(this.scope.isInScope("Please refund the duplicate charge.")).isTrue();
		assertThat(this.scope.isInScope("I need a human agent.")).isTrue();
		assertThat(this.scope.isInScope("What is on my invoice?")).isTrue();
	}

	@Test
	void rejectsUnrelatedRequests() {
		assertThat(this.scope.isInScope("Write me a poem about the ocean.")).isFalse();
		assertThat(this.scope.isInScope("What is the weather today?")).isFalse();
		assertThat(this.scope.isInScope("Tell me a joke.")).isFalse();
		assertThat(this.scope.isInScope("")).isFalse();
		assertThat(this.scope.isInScope(null)).isFalse();
	}

}
