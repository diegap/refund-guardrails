package dev.diegap.refundguardrails.domain;

import java.util.Optional;

/**
 * Narrow lookup port for resolving a card number to the account that owns it.
 * The card directory uses it without knowing where the account data lives.
 */
public interface AccountLookup {

	Optional<Account> accountForPan(String panDigits);

}
