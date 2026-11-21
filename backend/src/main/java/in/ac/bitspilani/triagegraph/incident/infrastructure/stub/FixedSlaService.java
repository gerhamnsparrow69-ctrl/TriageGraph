package in.ac.bitspilani.triagegraph.incident.infrastructure.stub;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.SlaTargets;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.SlaService;

import java.time.Duration;

/** Same targets for every service, by priority only. Replaced by M3's policy resolver (roadmap step 7). */
public final class FixedSlaService implements SlaService {

    static final int WARNING_PERCENT = 75;

    @Override
    public SlaTargets targetsFor(long serviceId, Priority p) {
        return switch (p) {
            case P1 -> targets(Duration.ofMinutes(15), Duration.ofHours(4), p);
            case P2 -> targets(Duration.ofMinutes(30), Duration.ofHours(8), p);
            case P3 -> targets(Duration.ofHours(4), Duration.ofHours(24), p);
            case P4 -> targets(Duration.ofHours(8), Duration.ofHours(72), p);
        };
    }

    private static SlaTargets targets(Duration response, Duration resolution, Priority p) {
        return new SlaTargets(response, resolution, WARNING_PERCENT, "Fixed default " + p);
    }
}
