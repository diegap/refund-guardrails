package dev.diegap.refundguardrails.assistant;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * A deliberately small, explainable prompt-injection heuristic.
 * <p>
 * It is not a security boundary. It catches overt attempts to rewrite the
 * assistant's rules; the refund policy, not this heuristic, is the authority on
 * money movement. The policy-override demonstration relies on a wording this
 * heuristic does not match.
 */
@Component
public final class InjectionHeuristic {

	private static final List<Pattern> PATTERNS = List.of(
			Pattern.compile("\\b(ignore|disregard|forget)\\b.{0,40}\\b(instructions?|rules?|prompts?|directions?)\\b",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\b(you are now|from now on you are|act as|pretend to be|roleplay as)\\b",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\b(developer mode|jailbreak|do anything now)\\b", Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\b(reveal|show|print|repeat)\\b.{0,20}\\b(system prompt|hidden instructions?|internal rules?)\\b",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\b(i am|i'm) (the )?(admin|administrator|developer|owner|system)\\b",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\b(grant|approve|authorize|allow|make)\\b.{0,30}\\b(exception|override|bypass)\\b",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\b(ignore|skip|bypass)\\b.{0,30}\\b(policy|limit|cap|checks?|approval)\\b",
					Pattern.CASE_INSENSITIVE),
			Pattern.compile("\\b(refund|approve|authorize)\\b.{0,20}\\b(no matter what|regardless of|without (any )?(checks|approval|verification))\\b",
					Pattern.CASE_INSENSITIVE));

	/** Returns a stable reason label when the text looks like an injection. */
	public Optional<String> detect(String text) {
		if (text == null || text.isBlank()) {
			return Optional.empty();
		}
		for (int index = 0; index < PATTERNS.size(); index++) {
			if (PATTERNS.get(index).matcher(text).find()) {
				return Optional.of("instruction_override_" + (index + 1));
			}
		}
		return Optional.empty();
	}

}
