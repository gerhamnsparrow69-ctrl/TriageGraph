package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class EvidenceAggregator {

    private static final Comparator<Evidence> ORDER = Comparator
            .comparingDouble(Evidence::bonus).reversed()
            .thenComparing(evidence -> evidence.type().name())
            .thenComparing(Evidence::text);

    private final double maximumBonus;

    public EvidenceAggregator(double maximumBonus) {
        if (!Double.isFinite(maximumBonus) || maximumBonus < 0) {
            throw new IllegalArgumentException("Maximum evidence bonus must be finite and non-negative");
        }
        this.maximumBonus = maximumBonus;
    }

    public AggregatedEvidence aggregate(Collection<Evidence> evidence) {
        Map<EvidenceType, Evidence> highestByType = new EnumMap<>(EvidenceType.class);
        for (Evidence candidate : evidence) {
            Evidence current = highestByType.get(candidate.type());
            if (current == null || ORDER.compare(candidate, current) < 0) {
                highestByType.put(candidate.type(), candidate);
            }
        }

        List<Evidence> retained = new ArrayList<>(highestByType.values());
        retained.sort(ORDER);
        double bonus = Math.min(maximumBonus,
                retained.stream().mapToDouble(Evidence::bonus).sum());
        return new AggregatedEvidence(retained, bonus);
    }
}
