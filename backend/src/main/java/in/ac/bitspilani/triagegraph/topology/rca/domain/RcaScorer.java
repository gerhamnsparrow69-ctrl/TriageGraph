package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RcaScorer {

    private static final Comparator<ScoredCandidate> RANKING_ORDER = Comparator
            .comparingLong((ScoredCandidate candidate) -> Math.round(candidate.score() * 1e9))
            .reversed()
            .thenComparing(Comparator.comparingDouble(ScoredCandidate::specificity).reversed())
            .thenComparingLong(ScoredCandidate::serviceId);

    private final RcaConfig config;

    public RcaScorer(RcaConfig config) {
        this.config = config;
    }

    public RcaAssessment assess(
            List<Long> candidateServiceIds,
            Set<Long> affectedServiceIds,
            ImpactTopology topology,
            Map<Long, String> serviceNames,
            Map<Long, AggregatedEvidence> evidenceByService) {
        List<ScoredCandidate> ranked = new ArrayList<>(candidateServiceIds.size());
        for (long candidateId : candidateServiceIds) {
            Set<Long> impact = new HashSet<>(topology.dependentsOf(candidateId));
            impact.add(candidateId);
            long affectedInImpact = affectedServiceIds.stream().filter(impact::contains).count();
            if (affectedInImpact == 0) {
                throw new IllegalStateException("Generated RCA candidate has no affected service in its impact");
            }

            double coverage = affectedInImpact / (double) affectedServiceIds.size();
            double specificity = affectedInImpact / (double) impact.size();
            AggregatedEvidence evidence = evidenceByService.getOrDefault(candidateId, AggregatedEvidence.none());
            double baseScore = config.coverageWeight() * coverage
                    + config.specificityWeight() * specificity;
            double score = Math.min(1.0, baseScore + evidence.bonus());
            ranked.add(new ScoredCandidate(
                    candidateId,
                    serviceNames.getOrDefault(candidateId, "Service " + candidateId),
                    score,
                    coverage,
                    specificity,
                    evidence.evidence().stream().map(Evidence::text).toList()));
        }

        ranked.sort(RANKING_ORDER);
        boolean ambiguous = isAmbiguous(ranked, config);
        return new RcaAssessment(ranked, ambiguous);
    }

    public static Comparator<ScoredCandidate> rankingOrder() {
        return RANKING_ORDER;
    }

    public static boolean isAmbiguous(List<ScoredCandidate> ranked, RcaConfig config) {
        return ranked.size() > 1
                && ranked.get(0).score() - ranked.get(1).score()
                <= config.ambiguityThreshold() + config.ambiguityEpsilon();
    }
}
