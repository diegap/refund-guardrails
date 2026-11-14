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

	@Bean
	ScriptedFixture nonRefundableFixture() {
		return ScriptedFixture.sequence("non-refundable",
				ScriptedResponse.toolCall("call_lookup_account_nonrefundable", "lookup_account",
						"{\"cardToken\":\"[[CARD_1]]\"}"),
				ScriptedResponse.toolCall("call_lookup_charges_nonrefundable", "lookup_charges", "{}"),
				ScriptedResponse.toolCall("call_issue_refund_nonrefundable", "issue_refund",
						"{\"chargeId\":\"ch_demo_final_sale\",\"amountCents\":9900}"),
				ScriptedResponse.text("That charge is not refundable. I can escalate this to a human agent."));
	}

	@Bean
	ScriptedFixture doubleRefundFixture() {
		return ScriptedFixture.sequence("double-refund",
				ScriptedResponse.toolCall("call_lookup_account_double", "lookup_account",
						"{\"cardToken\":\"[[CARD_1]]\"}"),
				ScriptedResponse.toolCall("call_lookup_charges_double", "lookup_charges", "{}"),
				ScriptedResponse.toolCall("call_issue_refund_double_1", "issue_refund",
						"{\"chargeId\":\"ch_demo_1\",\"amountCents\":4200}"),
				ScriptedResponse.toolCall("call_issue_refund_double_2", "issue_refund",
						"{\"chargeId\":\"ch_demo_1\",\"amountCents\":4200}"),
				ScriptedResponse.text("The first refund was approved; the second was refused because the "
						+ "charge is already refunded."));
	}

	@Bean
	ScriptedFixture overChargeFixture() {
		return ScriptedFixture.sequence("over-charge",
				ScriptedResponse.toolCall("call_lookup_account_overcharge", "lookup_account",
						"{\"cardToken\":\"[[CARD_1]]\"}"),
				ScriptedResponse.toolCall("call_lookup_charges_overcharge", "lookup_charges", "{}"),
				ScriptedResponse.toolCall("call_issue_refund_overcharge", "issue_refund",
						"{\"chargeId\":\"ch_demo_1\",\"amountCents\":5000}"),
				ScriptedResponse.text("That amount is higher than the refundable balance of the charge. "
						+ "I can escalate this to a human agent."));
	}

	@Bean
	ScriptedFixture nonPositiveFixture() {
		return ScriptedFixture.sequence("non-positive",
				ScriptedResponse.toolCall("call_lookup_account_nonpositive", "lookup_account",
						"{\"cardToken\":\"[[CARD_1]]\"}"),
				ScriptedResponse.toolCall("call_lookup_charges_nonpositive", "lookup_charges", "{}"),
				ScriptedResponse.toolCall("call_issue_refund_nonpositive", "issue_refund",
						"{\"chargeId\":\"ch_demo_1\",\"amountCents\":0}"),
				ScriptedResponse.text("A refund needs a positive amount. I can escalate this to a human agent."));
	}

	@Bean
	ScriptedFixture noAccountFixture() {
		return ScriptedFixture.sequence("no-account",
				ScriptedResponse.toolCall("call_issue_refund_noaccount", "issue_refund",
						"{\"chargeId\":\"ch_demo_1\",\"amountCents\":4200}"),
				ScriptedResponse.text("I could not verify the account for this conversation. "
						+ "I can escalate this to a human agent."));
	}

	@Bean
	ScriptedFixture teamPlanRefundFixture() {
		return ScriptedFixture.sequence("team-plan-refund",
				ScriptedResponse.toolCall("call_lookup_account_team", "lookup_account",
						"{\"cardToken\":\"[[CARD_1]]\"}"),
				ScriptedResponse.toolCall("call_lookup_charges_team", "lookup_charges", "{}"),
				ScriptedResponse.toolCall("call_issue_refund_team", "issue_refund",
						"{\"chargeId\":\"ch_demo_2\",\"amountCents\":12900}"),
				ScriptedResponse.text("I issued a refund for the Team plan charge."));
	}

}
