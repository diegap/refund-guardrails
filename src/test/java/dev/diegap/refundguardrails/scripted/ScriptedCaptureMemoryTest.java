package dev.diegap.refundguardrails.scripted;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 3.4: captured requests stay in memory. A marker sent in the body must
 * not appear in the logs or in any regular file under the JVM temp directory.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("mock")
class ScriptedCaptureMemoryTest {

	private static final Path TEMP_DIRECTORY;

	private static final String PREVIOUS_TMP_DIRECTORY;

	static {
		try {
			TEMP_DIRECTORY = Files.createTempDirectory("refund-guardrails-capture-");
			PREVIOUS_TMP_DIRECTORY = System.getProperty("java.io.tmpdir");
			System.setProperty("java.io.tmpdir", TEMP_DIRECTORY.toString());
		}
		catch (IOException ex) {
			throw new ExceptionInInitializerError(ex);
		}
	}

	@LocalServerPort
	private int port;

	private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

	@BeforeEach
	void attachLogs() {
		this.logs.start();
		((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).addAppender(this.logs);
	}

	@AfterEach
	void detachLogs() {
		((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)).detachAppender(this.logs);
		this.logs.stop();
	}

	@AfterAll
	static void restoreTempDirectory() {
		if (PREVIOUS_TMP_DIRECTORY != null) {
			System.setProperty("java.io.tmpdir", PREVIOUS_TMP_DIRECTORY);
		}
	}

	@Test
	void capturedBodyIsNeitherLoggedNorWrittenToDisk() throws Exception {
		String marker = "capture-marker-d41d8cd9";
		String body = """
				{"model":"gpt-4o-mini","messages":[
				  {"role":"system","content":"[[scenario:off-topic]][[session:memory-only]]"},
				  {"role":"user","content":"%s"}
				],"stream":false}
				""".formatted(marker);

		HttpRequest request = HttpRequest
			.newBuilder(URI.create("http://localhost:" + this.port + "/v1/chat/completions"))
			.header("Content-Type", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(body))
			.build();
		HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(this.logs.list).noneMatch((event) -> event.getFormattedMessage().contains(marker));
		assertThat(filesContaining(TEMP_DIRECTORY, marker)).isEmpty();
	}

	private static List<Path> filesContaining(Path directory, String marker) throws IOException {
		try (Stream<Path> files = Files.walk(directory)) {
			return files.filter(Files::isRegularFile).filter((file) -> contains(file, marker)).toList();
		}
	}

	private static boolean contains(Path file, String marker) {
		try {
			return Files.readString(file).contains(marker);
		}
		catch (IOException | RuntimeException ex) {
			return false;
		}
	}

}
