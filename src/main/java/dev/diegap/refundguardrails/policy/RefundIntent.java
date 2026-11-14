package dev.diegap.refundguardrails.policy;

/** A refund the model asked for, expressed in domain identifiers only. */
public record RefundIntent(String sessionId, String accountId, String chargeId, long amountCents) {

}
