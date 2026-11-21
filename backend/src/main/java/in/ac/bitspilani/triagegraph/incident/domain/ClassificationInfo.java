package in.ac.bitspilani.triagegraph.incident.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.ClassificationSuggestion;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Where the category and service came from (KEYWORD, KNN, LLM, USER or FALLBACK) and how sure we were. */
@Embeddable
public class ClassificationInfo {

    @Column(name = "classification_source", length = 20)
    private String source;

    @Column(name = "classification_confidence")
    private Double confidence;

    @Column(name = "classification_explanation", length = 500)
    private String explanation;

    protected ClassificationInfo() {
    }

    public ClassificationInfo(String source, double confidence, String explanation) {
        this.source = source;
        this.confidence = confidence;
        this.explanation = explanation;
    }

    public static ClassificationInfo from(ClassificationSuggestion s) {
        return new ClassificationInfo(s.source(), s.confidence(), s.explanation());
    }

    public String getSource() {
        return source;
    }

    public double getConfidence() {
        return confidence == null ? 0 : confidence;
    }

    public String getExplanation() {
        return explanation;
    }
}
