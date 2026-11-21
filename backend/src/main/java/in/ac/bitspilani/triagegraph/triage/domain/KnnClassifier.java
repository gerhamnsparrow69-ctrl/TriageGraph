package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Learns from history: the k most similar resolved incidents vote, each weighted by its similarity.
 * Confidence is the winning vote share, scaled down when even the closest neighbour is not very similar:
 * <pre>confidence = share × min(1, topSimilarity / 0.45)</pre>
 * (0.45 comes from the calibration on the seeded archive: correct neighbours averaged 0.54, wrong ones 0.48.)
 */
public class KnnClassifier implements IncidentClassifier {

    public static final String SOURCE = "KNN";
    static final double FULL_TRUST_SIMILARITY = 0.45;

    private final VectorIndex index;
    private final int k;

    public KnnClassifier(VectorIndex index, int k) {
        if (k < 1) {
            throw new IllegalArgumentException("k must be at least 1");
        }
        this.index = index;
        this.k = k;
    }

    @Override
    public String source() {
        return SOURCE;
    }

    @Override
    public Optional<ClassificationCandidate> classify(TriageText input) {
        List<ScoredView> neighbours = index.nearest(input.vector(), k,
                v -> v.isResolved() && v.categoryCode() != null);
        if (neighbours.isEmpty()) {
            return Optional.empty();
        }
        double reliability = Math.min(1.0, Math.max(0, neighbours.get(0).similarity()) / FULL_TRUST_SIMILARITY);
        Vote category = vote(neighbours, KnowledgeView::categoryCode);
        Vote service = vote(neighbours, KnowledgeView::serviceName);
        if (category == null) {
            return Optional.empty();
        }
        Long serviceId = service == null ? null : neighbours.stream().map(ScoredView::view)
                .filter(v -> service.key().equals(v.serviceName()) && v.serviceId() != null)
                .map(KnowledgeView::serviceId).findFirst().orElse(null);
        ScoredView closest = neighbours.get(0);
        String explanation = category.count() + " of " + neighbours.size() + " similar resolved incidents were "
                + category.key() + " (closest " + closest.view().code() + ", "
                + Math.round(closest.similarity() * 100) + "% similar)";
        return Optional.of(new ClassificationCandidate(category.key(), category.share() * reliability, serviceId,
                service == null ? null : service.key(), service == null ? 0 : service.share() * reliability,
                SOURCE, explanation));
    }

    private record Vote(String key, double share, int count) {
    }

    private static Vote vote(List<ScoredView> neighbours, Function<KnowledgeView, String> label) {
        Map<String, Double> weight = new LinkedHashMap<>();
        Map<String, Integer> count = new LinkedHashMap<>();
        double total = 0;
        for (ScoredView n : neighbours) {
            String key = label.apply(n.view());
            double w = Math.max(0, n.similarity());
            total += w;
            if (key != null) {
                weight.merge(key, w, Double::sum);
                count.merge(key, 1, Integer::sum);
            }
        }
        if (weight.isEmpty() || total == 0) {
            return null;
        }
        Map.Entry<String, Double> best = weight.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow();
        return new Vote(best.getKey(), best.getValue() / total, count.get(best.getKey()));
    }
}
