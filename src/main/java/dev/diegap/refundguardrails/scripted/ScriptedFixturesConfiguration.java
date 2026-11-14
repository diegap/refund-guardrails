package dev.diegap.refundguardrails.scripted;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Deterministic scripts. Tool-call arguments mirror the real tool schema; each
 * step is selected by how many tool messages the conversation already carries.
 * Fixture text is frozen: no timestamps or random identifiers.
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
		return ScriptedFixture.sequence("duplicate-charge",
				ScriptedResponse.toolCall("call_lookup_account_1", "lookup_account",
						"{\"cardToken\":\"[[CARD_1]]\"}"),
				ScriptedResponse.toolCall("call_lookup_charges_1", "lookup_charges", "{}"),
				ScriptedResponse.toolCall("call_issue_refund_1", "issue_refund",
						"{\"chargeId\":\"ch_demo_1\",\"amountCents\":4200}"),
				ScriptedResponse.text("I found the duplicate charge and issued a refund of $42.00. "
						+ "It should appear in 5-10 business days."));
	}

	@Bean
	ScriptedFixture overCapFixture() {
		return ScriptedFixture.sequence("over-cap-refund",
				ScriptedResponse.toolCall("call_lookup_account_2", "lookup_account",
						"{\"cardToken\":\"[[CARD_1]]\"}"),
				ScriptedResponse.toolCall("call_lookup_charges_2", "lookup_charges", "{}"),
				ScriptedResponse.toolCall("call_issue_refund_2", "issue_refund",
						"{\"chargeId\":\"ch_demo_over_cap\",\"amountCents\":50000}"),
				ScriptedResponse.text("That amount is above the automatic refund limit, so I cannot approve it. "
						+ "I can escalate this to a human agent."));
	}

	@Bean
	ScriptedFixture policyOverrideFixture() {
		return ScriptedFixture.sequence("policy-override",
				ScriptedResponse.toolCall("call_lookup_account_3", "lookup_account",
						"{\"cardToken\":\"[[CARD_1]]\"}"),
				ScriptedResponse.toolCall("call_lookup_charges_3", "lookup_charges", "{}"),
				ScriptedResponse.toolCall("call_issue_refund_override", "issue_refund",
						"{\"chargeId\":\"ch_demo_1\",\"amountCents\":999999}"),
				ScriptedResponse.toolCall("call_escalate_override", "escalate_to_human",
						"{\"reason\":\"policy exception requested\"}"),
				ScriptedResponse.text("I cannot override the refund policy. "
						+ "I have escalated your request to a human agent."));
	}

	@Bean
	ScriptedFixture offTopicFixture() {
		return ScriptedFixture.textOnly("off-topic",
				"I can only help with accounts, charges, refunds, or escalation.");
	}

	@Bean
	ScriptedFixture injectionFixture() {
		return ScriptedFixture.textOnly("injection",
				"I can only help with accounts, charges, refunds, or escalation.");
	}

	@Bean
	ScriptedFixture toolLoopFixture() {
		return ScriptedFixture.sequence("tool-loop",
				ScriptedResponse.toolCall("call_lookup_charges_loop", "lookup_charges",
						"{\"accountId\":\"acct_demo_1\"}"),
				ScriptedResponse.text("I found the duplicate charge and issued a refund within policy."));
	}

	@Bean
	ScriptedFixture toolErrorFixture() {
		return ScriptedFixture.sequence("tool-error",
				ScriptedResponse.toolCall("call_lookup_account_error", "lookup_account",
						"{\"cardToken\":\"[[CARD_1]]\"}"),
				ScriptedResponse.toolCall("call_issue_refund_error", "issue_refund",
						"{\"chargeId\":\"ch_demo_1\",\"amountCents\":\"not-a-number\"}"),
				ScriptedResponse.text("unreachable"));
	}

	@Bean
	ScriptedFixture unknownToolFixture() {
		return ScriptedFixture.sequence("unknown-tool",
				ScriptedResponse.toolCall("call_lookup_account_unknown", "lookup_account",
						"{\"cardToken\":\"[[CARD_1]]\"}"),
				ScriptedResponse.toolCall("call_delete_account", "delete_account", "{}"),
				ScriptedResponse.text("unreachable"));
	}

	@Bean
	ScriptedFixture providerErrorFixture() {
		return ScriptedFixture.failure("provider-error", 400, "provider_unavailable",
				"The scripted provider rejected the request.");
	}

}
