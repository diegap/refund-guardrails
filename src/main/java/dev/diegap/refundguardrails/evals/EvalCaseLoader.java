package dev.diegap.refundguardrails.evals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.yaml.snakeyaml.Yaml;

/**
 * Loads the declarative case file. A case is data: name, message, scenario, and
 * expectations. Adding one to the YAML requires no Java change.
 */
public final class EvalCaseLoader {

	private EvalCaseLoader() {
	}

	public static EvalSuite loadClasspath(String resource) {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try (InputStream input = loader.getResourceAsStream(resource)) {
			if (input == null) {
				throw new IllegalArgumentException("evaluation case file not found on the classpath: " + resource);
			}
			return parse(input);
		}
		catch (IOException ex) {
			throw new IllegalStateException("cannot read evaluation case file: " + resource, ex);
		}
	}

	public static EvalSuite load(Path path) {
		try (InputStream input = Files.newInputStream(path)) {
			return parse(input);
		}
		catch (IOException ex) {
			throw new IllegalStateException("cannot read evaluation case file: " + path, ex);
		}
	}

	@SuppressWarnings("unchecked")
	private static EvalSuite parse(InputStream input) {
		Object loaded = new Yaml().load(input);
		if (!(loaded instanceof Map<?, ?> rawRoot)) {
			throw new IllegalArgumentException("evaluation case file must be a mapping");
		}
		Map<String, Object> root = (Map<String, Object>) rawRoot;
		String suite = requiredString(root, "suite");
		String scope = requiredString(root, "scope");

		Object rawCases = root.get("cases");
		if (!(rawCases instanceof List<?> list)) {
			throw new IllegalArgumentException("evaluation case file needs a 'cases' list");
		}

		List<EvalCase> cases = new ArrayList<>();
		Set<String> names = new LinkedHashSet<>();
		for (Object rawCase : list) {
			if (!(rawCase instanceof Map<?, ?> rawMap)) {
				throw new IllegalArgumentException("each evaluation case must be a mapping");
			}
			EvalCase evalCase = toCase((Map<String, Object>) rawMap);
			if (!names.add(evalCase.name())) {
				throw new IllegalArgumentException("duplicate evaluation case name: " + evalCase.name());
			}
			cases.add(evalCase);
		}
		if (cases.isEmpty()) {
			throw new IllegalArgumentException("evaluation case file contains no cases");
		}
		return new EvalSuite(suite, scope, cases);
	}

	@SuppressWarnings("unchecked")
	private static EvalCase toCase(Map<String, Object> raw) {
		String name = requiredString(raw, "name");
		String message = requiredString(raw, "message");
		String scenario = nullableString(raw.get("scenario"));

		Object rawExpect = raw.get("expect");
		Map<String, Object> expect = rawExpect instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();

		Boolean noPan = nullableBoolean(expect.get("noPanInModelBound"));
		if (Boolean.FALSE.equals(noPan)) {
			throw new IllegalArgumentException(
					"case '" + name + "' cannot disable the privacy invariant (noPanInModelBound must be true)");
		}

		EvalExpectations expectations = new EvalExpectations(
				stringList(expect.get("guardsFired")), stringList(expect.get("guardsPassed")),
				nullableBoolean(expect.get("refusal")), stringList(expect.get("toolSequence")),
				stringList(expect.get("contains")), stringList(expect.get("omits")), noPan,
				stringList(expect.get("modelBoundContains")), nullableInteger(expect.get("ledgerRefunds")),
				nullableBoolean(expect.get("ledgerUnchanged")), nullableString(expect.get("failureCategory")));
		return new EvalCase(name, message, scenario, expectations);
	}

	private static String requiredString(Map<String, Object> map, String key) {
		String value = nullableString(map.get(key));
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("evaluation case is missing '" + key + "'");
		}
		return value;
	}

	private static String nullableString(Object value) {
		return value == null ? null : String.valueOf(value);
	}

	private static Boolean nullableBoolean(Object value) {
		return value == null ? null : Boolean.valueOf(String.valueOf(value));
	}

	private static Integer nullableInteger(Object value) {
		return value == null ? null : Integer.valueOf(String.valueOf(value));
	}

	private static List<String> stringList(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof List<?> list) {
			return list.stream().map(String::valueOf).toList();
		}
		return List.of(String.valueOf(value));
	}

}
