package dev.diegap.refundguardrails.domain;

import java.util.List;
import java.util.Optional;

/**
 * Read access to charges. Consumers depend on this port rather than on a
 * concrete seed or persistence adapter.
 */
public interface ChargeRepository {

	Optional<Charge> chargeById(String chargeId);

	List<Charge> chargesFor(String accountId);

}
