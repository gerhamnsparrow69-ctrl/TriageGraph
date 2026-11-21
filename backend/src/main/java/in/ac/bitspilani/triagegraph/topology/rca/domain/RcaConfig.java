package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.time.Duration;
import java.util.Objects;

public record RcaConfig(
        double coverageWeight,
        double specificityWeight,
        double recentChangeBonus,
        double openIncidentBonus,
        double maximumEvidenceBonus,
        Duration recentChangeWindow,
        double ambiguityThreshold,
        double ambiguityEpsilon) {

    public RcaConfig {
        Objects.requireNonNull(recentChangeWindow, "recentChangeWindow");
        if (!Double.isFinite(coverageWeight) || !Double.isFinite(specificityWeight)
                || !Double.isFinite(recentChangeBonus) || !Double.isFinite(openIncidentBonus)
                || !Double.isFinite(maximumEvidenceBonus) || !Double.isFinite(ambiguityThreshold)
                || !Double.isFinite(ambiguityEpsilon)
                || coverageWeight < 0 || specificityWeight < 0
                || recentChangeBonus < 0 || openIncidentBonus < 0
                || maximumEvidenceBonus < 0 || recentChangeWindow.isNegative()
                || ambiguityThreshold < 0 || ambiguityEpsilon < 0) {
            throw new IllegalArgumentException("RCA configuration values must be non-negative");
        }
    }

    public static RcaConfig defaults() {
        return new RcaConfig(0.60, 0.25, 0.10, 0.05, 0.15,
                Duration.ofHours(2), 0.05, 1e-9);
    }
}
