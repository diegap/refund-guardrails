package dev.diegap.refundguardrails.domain;

import java.util.Optional;

/**
 * Read access to accounts. Consumers depend on this port rather than on a
 * concrete seed or persistence adapter.
 */
public interface AccountRepository {

	Optional<Account> accountById(String accountId);

}
