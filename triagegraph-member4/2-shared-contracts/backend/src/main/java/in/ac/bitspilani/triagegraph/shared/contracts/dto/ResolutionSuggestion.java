package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import java.util.List;

public record ResolutionSuggestion(
        List<SimilarIncident> basedOn, String draft, boolean aiGenerated) {
}
