package dev.diegap.refundguardrails.domain;

import java.util.Map;
import java.util.stream.Collectors;

import dev.diegap.refundguardrails.testsupport.DocumentedTestPans;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoDataTest {

	private final DemoData data = new DemoData();

	@Test
	void seedsThreeAccountsWithDocumentedTestCards() {
		assertThat(this.data.accounts()).extracting(Account::id)
			.containsExactly("acct_demo_1", "acct_demo_2", "acct_demo_3");
		assertThat(this.data.accounts()).extracting((account) -> account.holderName())
			.containsExactly("Ada Lovelace", "Grace Hopper", "Alan Turing");

		for (Account account : this.data.accounts()) {
			String pan = account.card().pan();
			assertThat(DocumentedTestPans.isDocumented(pan)).as("documented test PAN: %s", pan).isTrue();
			assertThat(DocumentedTestPans.passesLuhn(pan)).as("Luhn: %s", pan).isTrue();
			assertThat(account.card().lastFour()).isEqualTo(pan.substring(pan.length() - 4));
		}
	}

	@Test
	void seedsFixedChargesAndAmounts() {
		Map<String, Long> amounts = this.data.charges()
			.stream()
			.collect(Collectors.toMap(Charge::id, Charge::amountCents));

		assertThat(amounts).containsExactlyInAnyOrderEntriesOf(Map.of(
				"ch_demo_0", 4200L,
				"ch_demo_1", 4200L,
				"ch_demo_2", 12900L,
				"ch_demo_over_cap", 50000L,
				"ch_demo_3", 2500L,
				"ch_demo_final_sale", 9900L));
		assertThat(this.data.charges()).allSatisfy((charge) -> {
			assertThat(charge.currency()).isEqualTo("USD");
			assertThat(this.data.account(charge.accountId())).isPresent();
		});
		assertThat(this.data.charge("ch_demo_final_sale")).get()
			.extracting(Charge::refundable)
			.isEqualTo(false);
		assertThat(this.data.charges())
			.filteredOn((charge) -> !charge.id().equals("ch_demo_final_sale"))
			.allSatisfy((charge) -> assertThat(charge.refundable()).isTrue());
	}

	@Test
	void everyAccountOwnsAtLeastOneCharge() {
		for (Account account : this.data.accounts()) {
			assertThat(this.data.chargesFor(account.id())).as(account.id()).isNotEmpty();
		}
	}

	@Test
	void ledgerStartsEmptyAndTotalsRefundsPerCharge() {
		assertThat(this.data.ledger().refunds()).isEmpty();
		assertThat(this.data.ledger().refundedAmountCents("ch_demo_1")).isZero();

		this.data.ledger().record(new Refund("rf_demo_1", "ch_demo_1", 1200, java.time.Instant.EPOCH));
		this.data.ledger().record(new Refund("rf_demo_2", "ch_demo_1", 300, java.time.Instant.EPOCH));

		assertThat(this.data.ledger().refundedAmountCents("ch_demo_1")).isEqualTo(1500);
		assertThat(this.data.ledger().refundedAmountCents("ch_demo_0")).isZero();
	}

}
