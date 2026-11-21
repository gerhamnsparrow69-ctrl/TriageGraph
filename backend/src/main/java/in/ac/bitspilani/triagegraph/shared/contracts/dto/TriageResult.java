package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import java.util.List;

public record TriageResult(
        ClassificationSuggestion suggestion, List<SimilarIncident> likelyDuplicates,
        List<SimilarIncident> possiblyRelated) {
}
