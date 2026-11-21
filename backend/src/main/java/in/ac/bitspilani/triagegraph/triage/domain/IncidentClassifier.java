package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.Optional;

/** Suggests a category and service for incident text. Empty when the classifier has no opinion. */
public interface IncidentClassifier {

    Optional<ClassificationCandidate> classify(TriageText input);

    /** KEYWORD, KNN or LLM. */
    String source();
}
