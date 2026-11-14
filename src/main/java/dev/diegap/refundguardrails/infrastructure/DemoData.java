package dev.diegap.refundguardrails.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import dev.diegap.refundguardrails.domain.Account;
import dev.diegap.refundguardrails.domain.AccountLookup;
import dev.diegap.refundguardrails.domain.AccountRepository;
import dev.diegap.refundguardrails.domain.Card;
import dev.diegap.refundguardrails.domain.Charge;
import dev.diegap.refundguardrails.domain.ChargeRepository;
import org.springframework.stereotype.Component;

/**
 * Deterministic in-memory seed. The card numbers are industry test numbers only
 * (see {@code DocumentedTestPans} in the tests); every other value is fixed so
 * tests and screenshots are stable.
 */
@Component
public class DemoData implements AccountRepository, ChargeRepository, AccountLookup {

	public static final String CURRENCY = "USD";

	private static final Instant JANUARY = Instant.parse("2026-01-05T09:00:00Z");

	private static final Instant FEBRUARY = Instant.parse("2026-02-11T14:30:00Z");

	private static final Instant MARCH = Instant.parse("2026-03-02T08:15:00Z");

	private final List<Account> accounts = List.of(
			new Account("acct_demo_1", "Ada Lovelace",
					new Card("4242424242424242", "4242", "Visa")),
			new Account("acct_demo_2", "Grace Hopper",
					new Card("5555555555554444", "4444", "Mastercard")),
			new Account("acct_demo_3", "Alan Turing",
					new Card("378282246310005", "0005", "American Express")));

	private final List<Charge> charges = List.of(
			new Charge("ch_demo_0", "acct_demo_1", 4200, CURRENCY, "Pro plan, annual", true, JANUARY),
			new Charge("ch_demo_1", "acct_demo_1", 4200, CURRENCY, "Pro plan, annual (duplicate)", true, JANUARY),
			new Charge("ch_demo_2", "acct_demo_2", 12900, CURRENCY, "Team plan, annual", true, FEBRUARY),
			new Charge("ch_demo_over_cap", "acct_demo_2", 50000, CURRENCY, "Enterprise onboarding", true, FEBRUARY),
			new Charge("ch_demo_3", "acct_demo_3", 2500, CURRENCY, "Priority support", true, MARCH),
			new Charge("ch_demo_final_sale", "acct_demo_3", 9900, CURRENCY, "Final-sale workshop", false, MARCH));

	public List<Account> accounts() {
		return this.accounts;
	}

	public List<Charge> charges() {
		return this.charges;
	}

	@Override
	public List<Charge> chargesFor(String accountId) {
		return this.charges.stream().filter((charge) -> charge.accountId().equals(accountId)).toList();
	}

	@Override
	public Optional<Account> accountById(String accountId) {
		return account(accountId);
	}

	@Override
	public Optional<Charge> chargeById(String chargeId) {
		return charge(chargeId);
	}

	public Optional<Account> account(String accountId) {
		return this.accounts.stream().filter((account) -> account.id().equals(accountId)).findFirst();
	}

	public Optional<Charge> charge(String chargeId) {
		return this.charges.stream().filter((charge) -> charge.id().equals(chargeId)).findFirst();
	}

	@Override
	public Optional<Account> accountForPan(String pan) {
		return this.accounts.stream().filter((account) -> account.card().pan().equals(pan)).findFirst();
	}

}
