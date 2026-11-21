package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import java.util.List;

public record RootCauseCandidate(
        long serviceId, String serviceName, double score, double coverage,
        double specificity, List<String> evidence) {
}
