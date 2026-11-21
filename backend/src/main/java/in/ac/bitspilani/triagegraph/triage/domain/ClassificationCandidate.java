package in.ac.bitspilani.triagegraph.triage.domain;

/**
 * One classifier's opinion. Category and service are judged separately because a classifier can be sure about
 * one and not the other (keywords may name the service but not the problem type). Confidences are in [0, 1].
 *
 * @param source KEYWORD, KNN, LLM, USER or FALLBACK (the values of the shared ClassificationSuggestion)
 */
public record ClassificationCandidate(String categoryCode, double confidence, Long serviceId, String serviceName,
        double serviceConfidence, String source, String explanation) {

    public ClassificationCandidate {
        confidence = clamp(confidence);
        serviceConfidence = clamp(serviceConfidence);
    }

    public boolean hasCategory() {
        return categoryCode != null && !categoryCode.isBlank();
    }

    public boolean hasService() {
        return serviceName != null && !serviceName.isBlank();
    }

    private static double clamp(double v) {
        return Double.isNaN(v) ? 0 : Math.max(0, Math.min(1, v));
    }
}
