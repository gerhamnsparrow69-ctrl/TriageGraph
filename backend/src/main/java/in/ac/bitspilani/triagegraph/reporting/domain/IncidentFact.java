package in.ac.bitspilani.triagegraph.reporting.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaStatus;

/**
 * One incident as the reporting side sees it: a flat, read-only row. Reports and search never touch the
 * incident aggregate; they work on these facts (CQRS read side, blueprint §10.4).
 */
public record IncidentFact(
        long id,
        String code,
        String title,
        String description,
        IncidentStatus status,
        Priority priority,
        String categoryCode,
        Long serviceId,
        String serviceName,
        long reporterId,
        String reporterName,
        Long assigneeId,
        String assigneeName,
        String classificationSource,
        Instant createdAt,
        Instant respondedAt,
        Instant resolvedAt,
        Instant responseDueAt,
        Instant resolutionDueAt,
        Instant slaStartedAt,
        Instant slaPausedAt,
        Integer slaWarningPercent,
        Long majorIncidentId,
        Long duplicateOfId,
        String rootCause,
        Set<Long> followerIds) {

    private static final Set<IncidentStatus> FINISHED =
            EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED, IncidentStatus.MERGED);
    private static final int DEFAULT_WARNING_PERCENT = 75;

    public IncidentFact {
        followerIds = followerIds == null ? Set.of() : Set.copyOf(followerIds);
    }

    /** Still needs work: not resolved, verified, closed or merged. */
    public boolean isOpen() {
        return !FINISHED.contains(status);
    }

    /** Resolved, verified or closed (a merged duplicate was never resolved on its own). */
    public boolean isResolved() {
        return resolvedAt != null && status != IncidentStatus.MERGED;
    }

    public boolean isCritical() {
        return priority == Priority.P1;
    }

    public boolean wasSuggestionAccepted() {
        return classificationSource != null && !"USER".equals(classificationSource);
    }

    /** Minutes from report to first response, when there was one. */
    public Optional<Duration> timeToAcknowledge() {
        return respondedAt == null ? Optional.empty() : Optional.of(Duration.between(createdAt, respondedAt));
    }

    /** Minutes from report to resolution, when it is resolved. */
    public Optional<Duration> timeToResolve() {
        return resolvedAt == null ? Optional.empty() : Optional.of(Duration.between(createdAt, resolvedAt));
    }

    /**
     * SLA status of the resolution target at {@code now}: MET or BREACHED once resolved, otherwise PAUSED,
     * BREACHED, AT_RISK (past the warning percentage of the window) or ON_TRACK. No target means ON_TRACK.
     */
    public SlaStatus slaStatus(Instant now) {
        if (resolutionDueAt == null) {
            return isOpen() ? SlaStatus.ON_TRACK : SlaStatus.MET;
        }
        if (!isOpen()) {
            return resolvedAt != null && resolvedAt.isAfter(resolutionDueAt) ? SlaStatus.BREACHED : SlaStatus.MET;
        }
        if (slaPausedAt != null) {
            return SlaStatus.PAUSED;
        }
        if (now.isAfter(resolutionDueAt)) {
            return SlaStatus.BREACHED;
        }
        Instant start = slaStartedAt != null ? slaStartedAt : createdAt;
        long window = Duration.between(start, resolutionDueAt).toMillis();
        long used = Duration.between(start, now).toMillis();
        int warning = slaWarningPercent == null ? DEFAULT_WARNING_PERCENT : slaWarningPercent;
        return window > 0 && used * 100 >= window * warning ? SlaStatus.AT_RISK : SlaStatus.ON_TRACK;
    }

    public boolean slaBreached(Instant now) {
        return slaStatus(now) == SlaStatus.BREACHED;
    }

    /** Who may see this row: the reporter, followers who joined it, and the assignee. */
    public boolean involves(long userId) {
        return reporterId == userId || followerIds.contains(userId) || (assigneeId != null && assigneeId == userId);
    }
}
