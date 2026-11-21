package in.ac.bitspilani.triagegraph.reporting.application;

import java.time.Instant;

import in.ac.bitspilani.triagegraph.reporting.domain.IncidentFact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaStatus;

/** One line of a search result or CSV export, with the SLA status worked out at the time of the search. */
public record IncidentRow(
        long id,
        String code,
        String title,
        IncidentStatus status,
        Priority priority,
        String categoryCode,
        Long serviceId,
        String serviceName,
        long reporterId,
        String reporterName,
        Long assigneeId,
        String assigneeName,
        SlaStatus slaStatus,
        Instant resolutionDueAt,
        boolean major,
        Instant createdAt,
        Instant resolvedAt) {

    static IncidentRow from(IncidentFact i, Instant now) {
        return new IncidentRow(i.id(), i.code(), i.title(), i.status(), i.priority(), i.categoryCode(),
                i.serviceId(), i.serviceName(), i.reporterId(), i.reporterName(), i.assigneeId(), i.assigneeName(),
                i.slaStatus(now), i.resolutionDueAt(), i.majorIncidentId() != null, i.createdAt(), i.resolvedAt());
    }
}
