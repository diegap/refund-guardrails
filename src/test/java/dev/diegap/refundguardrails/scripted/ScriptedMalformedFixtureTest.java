package dev.diegap.refundguardrails.scripted;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mock")
@Import(ScriptedMalformedFixtureTest.BrokenFixture.class)
class ScriptedMalformedFixtureTest {

	@LocalServerPort
	private int port;

	@Autowired
	private JsonMapper jsonMapper;

	@Test
	void malformedToolArgumentsFailLoudly() throws Exception {
		String body = """
				{"model":"gpt-4o-mini","messages":[
				  {"role":"system","content":"[[scenario:malformed-arguments]]"},
				  {"role":"user","content":"Refund it"}
				],"stream":false}
				""";

		HttpRequest request = HttpRequest
			.newBuilder(URI.create("http://localhost:" + this.port + "/v1/chat/completions"))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(500);
		assertThat(this.jsonMapper.readTree(response.body()).path("error").path("code").asString())
			.isEqualTo("invalid_fixture");
		assertThat(this.jsonMapper.readTree(response.body()).path("error").path("message").asString())
			.contains("malformed-arguments");
	}

	@TestConfiguration
	static class BrokenFixture {

		@Bean
		ScriptedFixture malformedArgumentsFixture() {
			return ScriptedFixture.sequence("malformed-arguments",
					ScriptedResponse.toolCall("call_bad", "lookup_charges", "{not-json"),
					ScriptedResponse.text("unreachable"));
		}

	}

}
