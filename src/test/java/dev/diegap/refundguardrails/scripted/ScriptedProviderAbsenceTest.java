package dev.diegap.refundguardrails.scripted;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ScriptedProviderAbsenceTest {

	@LocalServerPort
	private int port;

	@Test
	void chatCompletionsPathIsNotServedWithoutMockProfile() throws Exception {
		HttpRequest request = HttpRequest
			.newBuilder(URI.create("http://localhost:" + this.port + "/v1/chat/completions"))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString("{\"messages\":[]}"))
			.build();

		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(404);
	}

}
