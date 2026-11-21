package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.Map;
import java.util.Optional;

/**
 * Port to a large language model (Gemini today). The domain only ever sees this interface; adapters and
 * decorators (cache, circuit breaker, no-op) live in infrastructure. The LLM is advisory: it suggests
 * categories and drafts resolutions, it never decides priority.
 * <p>
 * Implementations may throw {@link in.ac.bitspilani.triagegraph.shared.exception.LlmUnavailableException};
 * callers treat any failure as "no opinion".
 */
public interface LlmClient {

    Optional<String> generateText(LlmPrompt prompt);

    /** Structured output: the model must answer with JSON matching the schema; returns the parsed object. */
    Optional<Map<String, Object>> generateJson(LlmPrompt prompt, Map<String, Object> responseSchema);

    String name();

    /** False when no API key is configured or the LLM is switched off. */
    default boolean enabled() {
        return true;
    }

    /** Human-readable state for the admin status page; decorators add their own details. */
    default String status() {
        return enabled() ? name() + ": ready" : name() + ": off";
    }
}
