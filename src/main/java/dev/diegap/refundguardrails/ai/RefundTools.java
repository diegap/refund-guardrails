package dev.diegap.refundguardrails.ai;

import java.util.List;
import java.util.Optional;

import dev.diegap.refundguardrails.assistant.EscalationQueue;
import dev.diegap.refundguardrails.assistant.EscalationResult;
import dev.diegap.refundguardrails.assistant.SessionAccounts;
import dev.diegap.refundguardrails.assistant.TurnContext;
import dev.diegap.refundguardrails.domain.Account;
import dev.diegap.refundguardrails.domain.AccountRepository;
import dev.diegap.refundguardrails.domain.CardSummary;
import dev.diegap.refundguardrails.domain.Charge;
import dev.diegap.refundguardrails.domain.ChargeRepository;
import dev.diegap.refundguardrails.domain.RefundLedger;
import dev.diegap.refundguardrails.policy.DenialReason;
import dev.diegap.refundguardrails.policy.RefundIntent;
import dev.diegap.refundguardrails.policy.RefundOutcome;
import dev.diegap.refundguardrails.policy.RefundService;
import dev.diegap.refundguardrails.security.CardDirectory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * The assistant's entire tool surface: four methods. None returns a card number;
 * card lookups resolve to a last-four projection. The model cannot widen its
 * authority: a session must first bind to an account, and every refund is still
 * decided by {@link RefundService}.
 */
@Component
public class RefundTools {

	private final CardDirectory cards;

	private final SessionAccounts sessions;

	private final AccountRepository accounts;

	private final ChargeRepository charges;

	private final RefundLedger ledger;

	private final RefundService refunds;

	private final EscalationQueue escalations;

	public RefundTools(CardDirectory cards, SessionAccounts sessions, AccountRepository accounts,
			ChargeRepository charges, RefundLedger ledger, RefundService refunds, EscalationQueue escalations) {
		this.cards = cards;
		this.sessions = sessions;
		this.accounts = accounts;
		this.charges = charges;
		this.ledger = ledger;
		this.refunds = refunds;
		this.escalations = escalations;
	}

	@Tool(name = "lookup_account",
			description = "Find the account for a card token from the user's message. Returns the account id, "
					+ "holder name, card brand, and card last four. Never returns a full card number.")
	public AccountView lookupAccount(
			@ToolParam(description = "Card token found in the user's message, for example [[CARD_1]]") String cardToken,
			ToolContext context) {
		TurnContext turn = turn(context);
		turn.toolInvoked("lookup_account");

		Optional<String> accountId = this.cards.accountIdFor(turn.sessionId(), cardToken);
		Optional<CardSummary> summary = this.cards.resolve(turn.sessionId(), cardToken);
		if (accountId.isEmpty() || summary.isEmpty()) {
			return AccountView.notFound();
		}
		Account account = this.accounts.accountById(accountId.get()).orElseThrow();
		this.sessions.bind(turn.sessionId(), account.id());
		return AccountView.found(account.id(), account.holderName(), summary.get().brand(), summary.get().lastFour());
	}

	@Tool(name = "lookup_charges",
			description = "List the charges for the account already bound to this session. Call lookup_account first.")
	public List<ChargeView> lookupCharges(ToolContext context) {
		TurnContext turn = turn(context);
		turn.toolInvoked("lookup_charges");

		Optional<String> accountId = this.sessions.accountId(turn.sessionId());
		if (accountId.isEmpty()) {
			return List.of();
		}
		Account account = this.accounts.accountById(accountId.get()).orElse(null);
		if (account == null) {
			return List.of();
		}
		return this.charges.chargesFor(account.id())
			.stream()
			.map((charge) -> view(charge, account))
			.toList();
	}

	@Tool(name = "issue_refund",
			description = "Request a refund for a charge. The server decides whether to approve it; "
					+ "never claim success unless the result status is 'approved'.")
	public RefundToolResult issueRefund(
			@ToolParam(description = "Charge identifier from lookup_charges") String chargeId,
			@ToolParam(description = "Amount to refund in cents") long amountCents, ToolContext context) {
		TurnContext turn = turn(context);
		turn.toolInvoked("issue_refund");

		Optional<String> accountId = this.sessions.accountId(turn.sessionId());
		if (accountId.isEmpty()) {
			return RefundToolResult.denied(chargeId, amountCents, DenialReason.ACCOUNT_MISMATCH.name(),
					"I could not verify the account for this conversation. I can escalate this to a human agent.");
		}

		RefundOutcome outcome = this.refunds.request(
				new RefundIntent(turn.sessionId(), accountId.get(), chargeId, amountCents));
		if (outcome instanceof RefundOutcome.Approved approved) {
			return RefundToolResult.approved(approved.refundId(), approved.chargeId(), approved.amountCents());
		}
		RefundOutcome.Denied denied = (RefundOutcome.Denied) outcome;
		return RefundToolResult.denied(chargeId, amountCents, denied.reason().name(), denied.message());
	}

	@Tool(name = "escalate_to_human",
			description = "Create a human-support escalation for this session. Use it when policy refuses a refund "
					+ "or when the user asks for a person.")
	public EscalationResult escalateToHuman(
			@ToolParam(description = "Short reason for the escalation") String reason, ToolContext context) {
		TurnContext turn = turn(context);
		turn.toolInvoked("escalate_to_human");
		return this.escalations.create(turn.sessionId(), reason);
	}

	private ChargeView view(Charge charge, Account account) {
		return new ChargeView(charge.id(), charge.amountCents(), charge.currency(), charge.description(),
				charge.chargedAt().toString(), status(charge), account.card().lastFour());
	}

	private String status(Charge charge) {
		if (!charge.refundable()) {
			return "not_refundable";
		}
		long refunded = this.ledger.refundedAmountCents(charge.id());
		if (refunded >= charge.amountCents()) {
			return "refunded";
		}
		return refunded > 0 ? "partially_refunded" : "refundable";
	}

	private static TurnContext turn(ToolContext context) {
		Object value = context == null ? null : context.getContext().get(TurnContext.KEY);
		if (value instanceof TurnContext turn) {
			return turn;
		}
		throw new IllegalStateException("tools require a turn context");
	}

}
