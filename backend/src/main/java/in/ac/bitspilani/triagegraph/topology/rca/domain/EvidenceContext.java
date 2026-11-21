package in.ac.bitspilani.triagegraph.topology.rca.domain;

import java.time.Instant;
import java.util.Objects;

public record EvidenceContext(Instant now) {

    public EvidenceContext {
        Objects.requireNonNull(now, "now");
    }
}
