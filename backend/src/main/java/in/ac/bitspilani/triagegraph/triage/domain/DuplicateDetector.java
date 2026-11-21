package in.ac.bitspilani.triagegraph.triage.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Intelligent Feature A: semantic duplicate detection. Compares a new report with open live incidents from the
 * last window (72 h by default):
 * <pre>
 * score = 0.80 · cosine(meaning vectors) + 0.10 · sameService + 0.10 · recency
 * recency = 1 − age / window
 * ≥ likely threshold   → "Likely duplicate" (banner + Join)
 * ≥ possibly threshold → "Possibly related" (side list)
 * </pre>
 * Meaning, not words: "Fee payment not going through" matches "Fee payment app unavailable" (score 0.83).
 */
public class DuplicateDetector {

    public static final double W_MEANING = 0.80;
    public static final double W_SERVICE = 0.10;
    public static final double W_RECENCY = 0.10;
    private static final int CANDIDATES = 50;

    public record Match(KnowledgeView view, double cosine, double score, boolean sameService, long minutesAgo) {
    }

    public record Report(List<Match> likely, List<Match> possibly) {

        public static Report empty() {
            return new Report(List.of(), List.of());
        }
    }

    private final VectorIndex index;

    public DuplicateDetector(VectorIndex index) {
        this.index = index;
    }

    /**
     * @param serviceId   affected service if known (chosen by the user or suggested)
     * @param serviceName same, by name (used when the catalog has no ids)
     * @param excludeIncidentId the incident itself, when checking an existing incident
     */
    public Report find(float[] query, Long serviceId, String serviceName, Instant now, DuplicatePolicy policy,
            Long excludeIncidentId) {
        Instant since = now.minus(policy.window());
        List<ScoredView> candidates = index.nearest(query, CANDIDATES, v -> v.source() == KnowledgeSource.LIVE
                && v.isOpen() && v.createdAt() != null && !v.createdAt().isBefore(since)
                && (excludeIncidentId == null || !excludeIncidentId.equals(v.incidentId())));
        List<Match> likely = new ArrayList<>();
        List<Match> possibly = new ArrayList<>();
        for (ScoredView c : candidates) {
            KnowledgeView v = c.view();
            boolean same = (serviceId != null && serviceId.equals(v.serviceId()))
                    || (serviceName != null && serviceName.equalsIgnoreCase(v.serviceName()));
            Duration age = Duration.between(v.createdAt(), now);
            double recency = recency(age, policy.window());
            double score = combinedScore(c.similarity(), same, recency);
            Match m = new Match(v, Vectors.round3(c.similarity()), Vectors.round3(score), same, Math.max(0, age.toMinutes()));
            if (score >= policy.likelyThreshold()) {
                likely.add(m);
            } else if (score >= policy.possiblyThreshold()) {
                possibly.add(m);
            }
        }
        Comparator<Match> best = Comparator.comparingDouble(Match::score).reversed();
        return new Report(likely.stream().sorted(best).limit(policy.maxResults()).toList(),
                possibly.stream().sorted(best).limit(policy.maxResults()).toList());
    }

    public static double combinedScore(double cosine, boolean sameService, double recency) {
        return W_MEANING * cosine + W_SERVICE * (sameService ? 1 : 0) + W_RECENCY * recency;
    }

    public static double recency(Duration age, Duration window) {
        double r = 1.0 - (double) Math.max(0, age.toSeconds()) / Math.max(1, window.toSeconds());
        return Math.max(0, Math.min(1, r));
    }
}
