package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.util.Objects;

public record Evidence(EvidenceType type, double bonus, String text) {

    public Evidence {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(text, "text");
        if (!Double.isFinite(bonus) || bonus < 0) {
            throw new IllegalArgumentException("Evidence bonus must be finite and non-negative");
        }
    }
}
