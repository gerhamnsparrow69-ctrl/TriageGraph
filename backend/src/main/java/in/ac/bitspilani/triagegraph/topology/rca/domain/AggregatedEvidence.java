package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.util.List;

public record AggregatedEvidence(List<Evidence> evidence, double bonus) {

    public AggregatedEvidence {
        evidence = List.copyOf(evidence);
        if (!Double.isFinite(bonus) || bonus < 0) {
            throw new IllegalArgumentException("Aggregated bonus must be finite and non-negative");
        }
    }

    public static AggregatedEvidence none() {
        return new AggregatedEvidence(List.of(), 0);
    }
}
