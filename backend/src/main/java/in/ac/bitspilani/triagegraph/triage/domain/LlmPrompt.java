package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.Objects;

/** A request to a language model. Incident text inside {@code user} must already be PII-masked. */
public record LlmPrompt(String system, String user, double temperature, int maxOutputTokens) {

    public LlmPrompt {
        Objects.requireNonNull(system);
        Objects.requireNonNull(user);
        if (temperature < 0 || temperature > 2) {
            throw new IllegalArgumentException("temperature must be between 0 and 2");
        }
        if (maxOutputTokens < 1) {
            throw new IllegalArgumentException("maxOutputTokens must be positive");
        }
    }
}
