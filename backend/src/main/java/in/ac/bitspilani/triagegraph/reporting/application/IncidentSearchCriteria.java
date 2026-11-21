package in.ac.bitspilani.triagegraph.reporting.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;

import in.ac.bitspilani.triagegraph.reporting.domain.IncidentSpecification;
import in.ac.bitspilani.triagegraph.reporting.domain.IncidentSpecifications;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaStatus;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;

/**
 * The 11 search criteria (blueprint §10.3; the brief asks for at least 5). Every field is optional; the ones
 * that are set are combined with AND. Dates are calendar days in the campus time zone, both ends inclusive.
 */
public record IncidentSearchCriteria(
        String code,
        LocalDate from,
        LocalDate to,
        Set<IncidentStatus> statuses,
        Set<Priority> priorities,
        String category,
        Long serviceId,
        Long technicianId,
        SlaStatus slaStatus,
        Long reporterId,
        Boolean major,
        String text) {

    public static final ZoneId CAMPUS_ZONE = ZoneId.of("Asia/Kolkata");

    public static IncidentSearchCriteria none() {
        return new IncidentSearchCriteria(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public IncidentSpecification toSpecification(Instant now) {
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidInputException("The 'to' date is before the 'from' date.");
        }
        IncidentSpecification spec = IncidentSpecification.any();
        if (hasText(code)) {
            spec = spec.and(IncidentSpecifications.code(code));
        }
        if (from != null) {
            spec = spec.and(IncidentSpecifications.reportedFrom(from.atStartOfDay(CAMPUS_ZONE).toInstant()));
        }
        if (to != null) {
            spec = spec.and(IncidentSpecifications.reportedBefore(to.plusDays(1).atStartOfDay(CAMPUS_ZONE).toInstant()));
        }
        if (statuses != null && !statuses.isEmpty()) {
            spec = spec.and(IncidentSpecifications.statusIn(statuses));
        }
        if (priorities != null && !priorities.isEmpty()) {
            spec = spec.and(IncidentSpecifications.priorityIn(priorities));
        }
        if (hasText(category)) {
            spec = spec.and(IncidentSpecifications.category(category));
        }
        if (serviceId != null) {
            spec = spec.and(IncidentSpecifications.service(serviceId));
        }
        if (technicianId != null) {
            spec = spec.and(IncidentSpecifications.assignedTo(technicianId));
        }
        if (slaStatus != null) {
            spec = spec.and(IncidentSpecifications.slaStatus(slaStatus, now));
        }
        if (reporterId != null) {
            spec = spec.and(IncidentSpecifications.reportedBy(reporterId));
        }
        if (major != null) {
            spec = spec.and(IncidentSpecifications.majorIncident(major));
        }
        if (hasText(text)) {
            spec = spec.and(IncidentSpecifications.text(text));
        }
        return spec;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
