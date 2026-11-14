package dev.diegap.refundguardrails.demo;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import dev.diegap.refundguardrails.assistant.EscalationQueue;
import dev.diegap.refundguardrails.domain.RefundLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("mock")
class DemoPageTest {

	private static final int PORT = freePort();

	@DynamicPropertySource
	static void serverPort(DynamicPropertyRegistry registry) {
		registry.add("server.port", () -> PORT);
	}

	@LocalServerPort
	private int port;

	@Autowired
	private RefundLedger ledger;

	@Autowired
	private EscalationQueue escalations;

	private final HttpClient http = HttpClient.newHttpClient();

	@BeforeEach
	void resetState() {
		this.ledger.clear();
		this.escalations.clear();
	}

	@Test
	void pageRendersTheFourScenariosAndTheInspectionPanel() throws Exception {
		HttpResponse<String> response = get("/");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("Duplicate charge", "Over-cap refund", "Policy override",
				"Off-topic request");
		assertThat(response.body()).contains("data-scenario=\"duplicate-charge\"",
				"data-scenario=\"over-cap-refund\"", "data-scenario=\"policy-override\"",
				"data-scenario=\"off-topic\"");
		assertThat(response.body()).contains("What the model saw", "hx-post=\"/demo/turns\"",
				"/vendor/htmx.min.js");
		assertThat(response.body()).contains("not PCI-compliant");
	}

	@Test
	void turnFragmentShowsRawInputAndTokenizedModelInput() throws Exception {
		Map<String, String> form = new LinkedHashMap<>();
		form.put("sessionId", "demo-page");
		form.put("message", "Please help with my card 4242 4242 4242 4242.");
		form.put("scenario", "");

		HttpResponse<String> response = postTurn(form);

		assertThat(response.statusCode()).isEqualTo(200);
		String html = response.body();
		assertThat(html).contains("Please help with my card 4242 4242 4242 4242.");
		assertThat(html).contains("[[CARD_1]]");
		assertThat(html).contains("Model-bound content");
		assertThat(html).contains("Raw input");
		assertThat(html).contains("card-number-redaction");
		assertThat(html).contains("INPUT");
		assertThat(html).contains("hx-swap-oob=\"innerHTML\"");
		assertThat(html).contains("Refund assistant");
	}

	@Test
	void scenarioButtonUsesTheCanonicalMessageAndRunsTheToolLoop() throws Exception {
		Map<String, String> form = new LinkedHashMap<>();
		form.put("sessionId", "demo-scenario");
		form.put("message", "");
		form.put("scenario", "duplicate-charge");

		HttpResponse<String> response = postTurn(form);

		assertThat(response.statusCode()).isEqualTo(200);
		String html = response.body();
		assertThat(html).contains("I was charged twice for my Pro plan");
		assertThat(html).contains("[[CARD_1]]");
		assertThat(html).contains("lookup_account", "lookup_charges", "issue_refund");
		assertThat(html).contains("Duplicate charge");
		assertThat(this.ledger.refunds()).hasSize(1);
	}

	@Test
	void scenarioDeepLinkRendersTheTurnOnFirstPaint() throws Exception {
		HttpResponse<String> response = get("/?scenario=duplicate-charge");

		assertThat(response.statusCode()).isEqualTo(200);
		String html = response.body();
		assertThat(html).contains("I was charged twice for my Pro plan", "[[CARD_1]]", "lookup_account",
				"lookup_charges", "issue_refund", "Duplicate charge");
		assertThat(html).doesNotContain("id=\"empty-state\"");
		assertThat(html).contains("What the model saw");
		assertThat(this.ledger.refunds()).hasSize(1);
	}

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + path)).GET().build();
		return this.http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private HttpResponse<String> postTurn(Map<String, String> form) throws Exception {
		String body = form.entrySet()
			.stream()
			.map((entry) -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
					+ URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
			.collect(Collectors.joining("&"));
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + this.port + "/demo/turns"))
			.header("Content-Type", "application/x-www-form-urlencoded")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		return this.http.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private static int freePort() {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
		catch (IOException ex) {
			throw new IllegalStateException("cannot allocate a local port", ex);
		}
	}

}
