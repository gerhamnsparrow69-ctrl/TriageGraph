package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.util.List;
import java.util.Objects;

public record ScoredCandidate(
        long serviceId,
        String serviceName,
        double score,
        double coverage,
        double specificity,
        List<String> evidence) {

    public ScoredCandidate {
        Objects.requireNonNull(serviceName, "serviceName");
        evidence = List.copyOf(evidence);
        if (!Double.isFinite(score) || !Double.isFinite(coverage) || !Double.isFinite(specificity)) {
            throw new IllegalArgumentException("RCA scores must be finite");
        }
    }
}
