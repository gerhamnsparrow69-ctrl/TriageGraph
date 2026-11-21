package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import java.time.Duration;

public record SlaTargets(
        Duration response, Duration resolution, int warningPercent, String policyName) {
}
