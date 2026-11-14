package dev.diegap.refundguardrails.assistant;

/** Model-facing account projection: no card number, only brand and last four. */
public record AccountView(String accountId, String holderName, String cardBrand, String cardLastFour, String error) {

	static AccountView found(String accountId, String holderName, String cardBrand, String cardLastFour) {
		return new AccountView(accountId, holderName, cardBrand, cardLastFour, null);
	}

	static AccountView notFound() {
		return new AccountView(null, null, null, null,
				"I could not find an account for that card token. Ask the user to check the card and try again.");
	}

}
