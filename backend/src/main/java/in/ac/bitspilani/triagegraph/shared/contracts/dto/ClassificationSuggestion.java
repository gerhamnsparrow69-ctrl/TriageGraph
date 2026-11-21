package in.ac.bitspilani.triagegraph.shared.contracts.dto;

/** {@code source} is one of KEYWORD, KNN, LLM, USER or FALLBACK. */
public record ClassificationSuggestion(
        String categoryCode, Long serviceId, double confidence,
        String source, String explanation) {
}
