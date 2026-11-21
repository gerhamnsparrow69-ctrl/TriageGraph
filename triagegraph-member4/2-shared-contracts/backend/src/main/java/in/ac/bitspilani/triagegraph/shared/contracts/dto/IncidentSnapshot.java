package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import java.time.Instant;
import java.util.Set;

public record IncidentSnapshot(
        long id, String code, String title, String description, IncidentStatus status,
        Priority priority, long serviceId, String categoryCode, long reporterId,
        Set<Long> followerIds, Long assigneeId, Long majorIncidentId,
        Instant createdAt, Instant responseDueAt, Instant resolutionDueAt) {
}
