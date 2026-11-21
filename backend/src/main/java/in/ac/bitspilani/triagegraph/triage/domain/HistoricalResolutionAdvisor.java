package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.List;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.ResolutionSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.SimilarIncident;

/**
 * The always-available layer: the k most similar resolved incidents and a draft built from the closest one.
 * Works offline and is fully deterministic.
 */
public class HistoricalResolutionAdvisor implements ResolutionAdvisor {

    private final VectorIndex index;
    private final double minSimilarity;

    public HistoricalResolutionAdvisor(VectorIndex index, double minSimilarity) {
        this.index = index;
        this.minSimilarity = minSimilarity;
    }

    @Override
    public ResolutionSuggestion advise(ResolutionQuery q) {
        List<ScoredView> similar = index.nearest(q.vector(), Math.max(1, q.k()),
                        v -> v.hasResolution() && (q.excludeDocId() == null || v.docId() != q.excludeDocId()))
                .stream().filter(s -> s.similarity() >= minSimilarity).toList();
        List<SimilarIncident> basedOn = similar.stream().map(s -> s.view().toSimilar(s.similarity())).toList();
        if (similar.isEmpty()) {
            return new ResolutionSuggestion(List.of(), "No similar resolved incident found yet.", false);
        }
        KnowledgeView top = similar.get(0).view();
        StringBuilder draft = new StringBuilder("Closest past fix (").append(top.code()).append(", ")
                .append(Math.round(similar.get(0).similarity() * 100)).append("% similar): ")
                .append(top.resolutionSummary().trim());
        if (top.rootCause() != null && !top.rootCause().isBlank()) {
            draft.append("\nRoot cause then: ").append(top.rootCause().trim()).append('.');
        }
        return new ResolutionSuggestion(basedOn, draft.toString(), false);
    }
}
