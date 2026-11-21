package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import java.util.List;

public record MajorIncidentDraft(
        List<Long> childIncidentIds, long rootCauseServiceId,
        double rcaScore, boolean ambiguous, String narrative) {
}
