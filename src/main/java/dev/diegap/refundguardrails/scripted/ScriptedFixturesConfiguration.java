package dev.diegap.refundguardrails.scripted;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * The four demonstration scenarios plus a safe default. Fixture text is frozen:
 * no timestamps or random identifiers, so repeated requests are byte-identical.
 */
@Configuration
@Profile("mock")
class ScriptedFixturesConfiguration {

	@Bean
	ScriptedFixture defaultFixture() {
		return ScriptedFixture.textOnly(ScriptedFixtureCatalog.DEFAULT_SCENARIO,
				"I can help with accounts, charges, refunds, or escalation to a human agent.");
	}

	@Bean
	ScriptedFixture duplicateChargeFixture() {
		return ScriptedFixture.withToolCall("duplicate-charge",
				ScriptedResponse.toolCall("call_lookup_charges_1", "lookup_charges",
						"{\"accountId\":\"acct_demo_1\"}"),
				"I found the duplicate charge and issued a refund within policy.");
	}

	@Bean
	ScriptedFixture overCapFixture() {
		return ScriptedFixture.withToolCall("over-cap-refund",
				ScriptedResponse.toolCall("call_issue_refund_1", "issue_refund",
						"{\"chargeId\":\"ch_demo_over_cap\",\"amountCents\":50000}"),
				"That amount is above the automatic refund limit, so I cannot approve it. "
						+ "I can escalate this to a human agent.");
	}

	@Bean
	ScriptedFixture policyOverrideFixture() {
		return ScriptedFixture.withToolCall("policy-override",
				ScriptedResponse.toolCall("call_issue_refund_override", "issue_refund",
						"{\"chargeId\":\"ch_demo_1\",\"amountCents\":999999}"),
				"I cannot override the refund policy. I have escalated your request to a human agent.");
	}

	@Bean
	ScriptedFixture offTopicFixture() {
		return ScriptedFixture.withToolCall("off-topic",
				ScriptedResponse.text("I can only help with accounts, charges, refunds, or escalation."),
				"I can only help with accounts, charges, refunds, or escalation.");
	}

}
