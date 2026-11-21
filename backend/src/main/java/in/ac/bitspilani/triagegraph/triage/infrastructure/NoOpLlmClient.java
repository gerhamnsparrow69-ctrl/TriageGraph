package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.util.Map;
import java.util.Optional;

import in.ac.bitspilani.triagegraph.triage.domain.LlmClient;
import in.ac.bitspilani.triagegraph.triage.domain.LlmPrompt;

/**
 * Null Object: used when no Gemini key is configured or the LLM is switched off. Callers need no
 * {@code if (llm != null)} checks; they simply get "no opinion" and fall back to the local classifiers.
 */
public class NoOpLlmClient implements LlmClient {

    private final String reason;

    public NoOpLlmClient(String reason) {
        this.reason = reason;
    }

    @Override
    public Optional<String> generateText(LlmPrompt prompt) {
        return Optional.empty();
    }

    @Override
    public Optional<Map<String, Object>> generateJson(LlmPrompt prompt, Map<String, Object> responseSchema) {
        return Optional.empty();
    }

    @Override
    public String name() {
        return "No LLM";
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public String status() {
        return "LLM off: " + reason;
    }
}
