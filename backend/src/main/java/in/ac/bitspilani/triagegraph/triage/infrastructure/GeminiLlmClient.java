package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import in.ac.bitspilani.triagegraph.shared.exception.LlmUnavailableException;
import in.ac.bitspilani.triagegraph.triage.domain.LlmClient;
import in.ac.bitspilani.triagegraph.triage.domain.LlmPrompt;

/**
 * Adapter from our {@link LlmClient} port to Google's Gemini REST API ({@code models/{model}:generateContent}).
 * Structured output uses {@code responseMimeType = application/json} with a {@code responseSchema}, so the
 * model must answer with JSON that matches our enums. The API key travels in the {@code x-goog-api-key} header
 * and comes only from the environment ({@code GEMINI_API_KEY}).
 */
public class GeminiLlmClient implements LlmClient {

    private final HttpClient http;
    private final String baseUrl;
    private final String model;
    private final String apiKey;
    private final Duration timeout;

    public GeminiLlmClient(String baseUrl, String model, String apiKey, Duration timeout) {
        this.baseUrl = stripTrailingSlash(Objects.requireNonNull(baseUrl));
        this.model = Objects.requireNonNull(model);
        this.apiKey = Objects.requireNonNull(apiKey);
        this.timeout = Objects.requireNonNull(timeout);
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public String name() {
        return "Gemini (" + model + ")";
    }

    @Override
    public Optional<String> generateText(LlmPrompt prompt) {
        return call(prompt, null);
    }

    @Override
    public Optional<Map<String, Object>> generateJson(LlmPrompt prompt, Map<String, Object> responseSchema) {
        return call(prompt, responseSchema).map(GeminiLlmClient::parseJsonAnswer);
    }

    private Optional<String> call(LlmPrompt prompt, Map<String, Object> schema) {
        String body = Json.write(requestBody(prompt, schema));
        HttpRequest request = HttpRequest.newBuilder(endpoint())
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new LlmUnavailableException("Gemini could not be reached: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmUnavailableException("Gemini call interrupted", e);
        }
        if (response.statusCode() / 100 != 2) {
            String detail = response.body() == null ? "" : response.body();
            throw new LlmUnavailableException("Gemini returned HTTP " + response.statusCode() + ": "
                    + (detail.length() > 200 ? detail.substring(0, 200) : detail));
        }
        return extractText(response.body());
    }

    URI endpoint() {
        return URI.create(baseUrl + "/models/" + URLEncoder.encode(model, StandardCharsets.UTF_8) + ":generateContent");
    }

    static Map<String, Object> requestBody(LlmPrompt prompt, Map<String, Object> schema) {
        Map<String, Object> generation = new LinkedHashMap<>();
        generation.put("temperature", prompt.temperature());
        generation.put("maxOutputTokens", prompt.maxOutputTokens());
        if (schema != null) {
            generation.put("responseMimeType", "application/json");
            generation.put("responseSchema", schema);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("systemInstruction", Map.of("parts", List.of(Map.of("text", prompt.system()))));
        body.put("contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", prompt.user())))));
        body.put("generationConfig", generation);
        return body;
    }

    /** Joins the text parts of the first candidate; empty when the answer was blocked or had no text. */
    static Optional<String> extractText(String responseBody) {
        Map<String, Object> root;
        try {
            root = Json.parseObject(responseBody);
        } catch (IllegalArgumentException e) {
            throw new LlmUnavailableException("Gemini sent a response that is not JSON", e);
        }
        if (!(root.get("candidates") instanceof List<?> candidates) || candidates.isEmpty()
                || !(candidates.get(0) instanceof Map<?, ?> first)
                || !(first.get("content") instanceof Map<?, ?> content)
                || !(content.get("parts") instanceof List<?> parts)) {
            return Optional.empty();
        }
        StringBuilder text = new StringBuilder();
        for (Object part : parts) {
            if (part instanceof Map<?, ?> p && p.get("text") instanceof String s) {
                text.append(s);
            }
        }
        return text.isEmpty() ? Optional.empty() : Optional.of(text.toString());
    }

    /** Parses the model's JSON answer, tolerating a ```json fence around it. */
    static Map<String, Object> parseJsonAnswer(String text) {
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("\\s*```$", "");
        }
        try {
            return Json.parseObject(t);
        } catch (IllegalArgumentException e) {
            throw new LlmUnavailableException("Gemini's answer was not valid JSON", e);
        }
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
