package in.ac.bitspilani.triagegraph.triage.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import in.ac.bitspilani.triagegraph.shared.exception.LlmUnavailableException;
import in.ac.bitspilani.triagegraph.triage.domain.LlmPrompt;

/** Runs the real adapter against a local HTTP server that imitates Gemini's generateContent endpoint. */
class GeminiLlmClientTest {

    private static final LlmPrompt PROMPT = new LlmPrompt("system text", "user text", 0.0, 128);

    private record Server(HttpServer http, AtomicReference<String> body, AtomicReference<String> key,
            AtomicReference<String> path) implements AutoCloseable {
        String url() {
            return "http://127.0.0.1:" + http.getAddress().getPort() + "/v1beta/";
        }

        @Override
        public void close() {
            http.stop(0);
        }
    }

    private static Server serve(int status, String response) throws IOException {
        HttpServer http = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var body = new AtomicReference<String>();
        var key = new AtomicReference<String>();
        var path = new AtomicReference<String>();
        http.createContext("/", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            key.set(exchange.getRequestHeaders().getFirst("x-goog-api-key"));
            path.set(exchange.getRequestURI().getPath());
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        http.start();
        return new Server(http, body, key, path);
    }

    private static String answer(String text) {
        return Json.write(Map.of("candidates", List.of(Map.of("content", Map.of("role", "model",
                "parts", List.of(Map.of("text", text)))))));
    }

    @Test
    void sendsKeyHeaderSystemPromptAndSchemaAndParsesTheJsonAnswer() throws IOException {
        try (Server s = serve(200, answer("```json\n{\"category\":\"APPLICATION\",\"confidence\":0.9}\n```"))) {
            var client = new GeminiLlmClient(s.url(), "gemini-flash-lite-latest", "secret-key", Duration.ofSeconds(5));
            Map<String, Object> schema = Map.of("type", "OBJECT");
            var result = client.generateJson(PROMPT, schema).orElseThrow();
            assertEquals("APPLICATION", result.get("category"));
            assertEquals(0.9, result.get("confidence"));
            assertEquals("secret-key", s.key().get());
            assertEquals("/v1beta/models/gemini-flash-lite-latest:generateContent", s.path().get());
            Map<String, Object> sent = Json.parseObject(s.body().get());
            assertTrue(s.body().get().contains("\"responseMimeType\":\"application/json\""));
            assertEquals("system text", ((Map<?, ?>) ((List<?>) ((Map<?, ?>) sent.get("systemInstruction")).get("parts")).get(0)).get("text"));
            assertTrue(!s.body().get().contains("secret-key"), "the key must not be in the body");
        }
    }

    @Test
    void textModeJoinsPartsAndSendsNoSchema() throws IOException {
        try (Server s = serve(200, answer("1. Restart the connector"))) {
            var client = new GeminiLlmClient(s.url(), "m", "k", Duration.ofSeconds(5));
            assertEquals("1. Restart the connector", client.generateText(PROMPT).orElseThrow());
            assertTrue(!s.body().get().contains("responseSchema"));
        }
    }

    /** TC19 (adapter side): quota errors and bad answers become LlmUnavailableException. */
    @Test
    void httpErrorsAndInvalidAnswersBecomeLlmUnavailable() throws IOException {
        try (Server s = serve(429, "{\"error\":{\"message\":\"quota exceeded\"}}")) {
            var client = new GeminiLlmClient(s.url(), "m", "k", Duration.ofSeconds(5));
            var e = assertThrows(LlmUnavailableException.class, () -> client.generateText(PROMPT));
            assertTrue(e.getMessage().startsWith("Gemini returned HTTP 429"));
        }
        try (Server s = serve(200, answer("not json at all"))) {
            var client = new GeminiLlmClient(s.url(), "m", "k", Duration.ofSeconds(5));
            assertThrows(LlmUnavailableException.class, () -> client.generateJson(PROMPT, Map.of()));
        }
    }

    @Test
    void blockedAnswerWithoutTextIsEmptyNotAnError() throws IOException {
        try (Server s = serve(200, "{\"candidates\":[{\"finishReason\":\"SAFETY\"}]}")) {
            assertTrue(new GeminiLlmClient(s.url(), "m", "k", Duration.ofSeconds(5)).generateText(PROMPT).isEmpty());
        }
    }

    @Test
    void unreachableServerBecomesLlmUnavailable() {
        var client = new GeminiLlmClient("http://127.0.0.1:1/v1beta", "m", "k", Duration.ofSeconds(2));
        assertThrows(LlmUnavailableException.class, () -> client.generateText(PROMPT));
    }
}
