package in.ac.bitspilani.triagegraph.reporting.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaStatus;

/** The search rules, one factory method per criterion. */
public final class IncidentSpecifications {

    private IncidentSpecifications() {
    }

    /** "INC-1004", "1004" or the database id. */
    public static IncidentSpecification code(String code) {
        String wanted = code.trim().toUpperCase(Locale.ROOT);
        return i -> i.code().equalsIgnoreCase(wanted) || i.code().equalsIgnoreCase("INC-" + wanted)
                || String.valueOf(i.id()).equals(wanted);
    }

    /** Reported at or after {@code from} (inclusive). */
    public static IncidentSpecification reportedFrom(Instant from) {
        return i -> !i.createdAt().isBefore(from);
    }

    /** Reported before {@code to} (exclusive). */
    public static IncidentSpecification reportedBefore(Instant to) {
        return i -> i.createdAt().isBefore(to);
    }

    public static IncidentSpecification statusIn(Set<IncidentStatus> statuses) {
        return i -> statuses.contains(i.status());
    }

    public static IncidentSpecification priorityIn(Set<Priority> priorities) {
        return i -> i.priority() != null && priorities.contains(i.priority());
    }

    public static IncidentSpecification category(String categoryCode) {
        return i -> categoryCode.equalsIgnoreCase(String.valueOf(i.categoryCode()));
    }

    public static IncidentSpecification service(long serviceId) {
        return i -> i.serviceId() != null && i.serviceId() == serviceId;
    }

    public static IncidentSpecification assignedTo(long technicianId) {
        return i -> i.assigneeId() != null && i.assigneeId() == technicianId;
    }

    public static IncidentSpecification slaStatus(SlaStatus status, Instant now) {
        return i -> i.slaStatus(now) == status;
    }

    public static IncidentSpecification reportedBy(long reporterId) {
        return i -> i.reporterId() == reporterId;
    }

    public static IncidentSpecification majorIncident(boolean major) {
        return i -> (i.majorIncidentId() != null) == major;
    }

    /** Free text: every word must appear in the code, title or description (case-insensitive). */
    public static IncidentSpecification text(String query) {
        String[] words = query.toLowerCase(Locale.ROOT).trim().split("\\s+");
        return i -> {
            String haystack = (i.code() + " " + i.title() + " " + i.description()).toLowerCase(Locale.ROOT);
            for (String word : words) {
                if (!haystack.contains(word)) {
                    return false;
                }
            }
            return true;
        };
    }

    /** Role scoping: what this viewer is allowed to see at all. */
    public static IncidentSpecification visibleTo(Viewer viewer) {
        return switch (viewer.role()) {
            case ADMIN, IT_MANAGER -> IncidentSpecification.any();
            case TEAM_LEAD -> i -> i.involves(viewer.userId())
                    || (i.assigneeId() != null && viewer.teamMemberIds().contains(i.assigneeId()));
            case TECHNICIAN -> i -> i.involves(viewer.userId());
            case EMPLOYEE -> i -> i.reporterId() == viewer.userId() || i.followerIds().contains(viewer.userId());
        };
    }
}
