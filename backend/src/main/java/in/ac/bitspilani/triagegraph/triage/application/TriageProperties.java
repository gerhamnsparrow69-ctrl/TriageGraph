package in.ac.bitspilani.triagegraph.triage.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import in.ac.bitspilani.triagegraph.triage.domain.DuplicatePolicy;

/**
 * Settings under {@code app.triage.*}. Thresholds here are only the starting values; admins change them at
 * runtime on the settings page (stored in {@code triage_settings}).
 *
 * @param embedder          {@code minilm} (default, falls back automatically) or {@code lexical}
 * @param repeatWindow      same reporter, near-identical text within this window → 409
 * @param repeatSimilarity  cosine at which a resubmission counts as the same text
 * @param knnNeighbours     k for the kNN classifier
 * @param minConfidence     below this the suggestion is shown as "please confirm"
 * @param fallbackCategory  category suggested when no classifier has an opinion
 */
@ConfigurationProperties(prefix = "app.triage")
public record TriageProperties(String embedder, Double likelyDuplicateThreshold, Double possiblyRelatedThreshold,
        Duration duplicateWindow, Duration repeatWindow, Double repeatSimilarity, Integer knnNeighbours,
        Double minConfidence, String fallbackCategory, Llm llm) {

    public TriageProperties {
        embedder = embedder == null || embedder.isBlank() ? "minilm" : embedder.trim();
        likelyDuplicateThreshold = likelyDuplicateThreshold == null ? DuplicatePolicy.DEFAULT_LIKELY : likelyDuplicateThreshold;
        possiblyRelatedThreshold = possiblyRelatedThreshold == null ? DuplicatePolicy.DEFAULT_POSSIBLY : possiblyRelatedThreshold;
        duplicateWindow = duplicateWindow == null ? Duration.ofHours(72) : duplicateWindow;
        repeatWindow = repeatWindow == null ? Duration.ofMinutes(10) : repeatWindow;
        repeatSimilarity = repeatSimilarity == null ? 0.95 : repeatSimilarity;
        knnNeighbours = knnNeighbours == null || knnNeighbours < 1 ? 5 : knnNeighbours;
        minConfidence = minConfidence == null ? 0.60 : minConfidence;
        fallbackCategory = fallbackCategory == null || fallbackCategory.isBlank() ? "SOFTWARE" : fallbackCategory.trim();
        llm = llm == null ? new Llm(null, null, null, null, null, null, null, null) : llm;
    }

    /**
     * Gemini settings ({@code app.triage.llm.*}). Without an API key the LLM is simply off.
     *
     * @param failureThreshold failures in a row before the circuit breaker opens
     * @param openDuration     how long calls are skipped once it is open
     */
    public record Llm(Boolean enabled, String apiKey, String model, String baseUrl, Duration timeout,
            Integer cacheSize, Integer failureThreshold, Duration openDuration) {

        public Llm {
            enabled = enabled == null || enabled;
            apiKey = apiKey == null ? "" : apiKey.trim();
            model = model == null || model.isBlank() ? "gemini-flash-lite-latest" : model.trim();
            baseUrl = baseUrl == null || baseUrl.isBlank() ? "https://generativelanguage.googleapis.com/v1beta" : baseUrl.trim();
            timeout = timeout == null ? Duration.ofSeconds(8) : timeout;
            cacheSize = cacheSize == null || cacheSize < 1 ? 500 : cacheSize;
            failureThreshold = failureThreshold == null || failureThreshold < 1 ? 3 : failureThreshold;
            openDuration = openDuration == null ? Duration.ofSeconds(60) : openDuration;
        }

        public boolean configured() {
            return enabled && !apiKey.isEmpty();
        }
    }
}
