package in.ac.bitspilani.triagegraph.incident.application;

import in.ac.bitspilani.triagegraph.incident.domain.Actor;
import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.incident.domain.InvestigationNote;
import in.ac.bitspilani.triagegraph.incident.domain.Resolution;
import in.ac.bitspilani.triagegraph.incident.domain.SlaDeadlines;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * What the API returns for one incident, tailored to the caller: {@code allowedActions} lists exactly
 * the buttons they may press, and employees never see internal notes.
 */
public record IncidentView(
        long id, String code, String title, String description, IncidentStatus status,
        Priority priority, Double priorityScore, String priorityRationale,
        Impact impact, Urgency urgency, int affectedUsers,
        String categoryCode, Long serviceId, String classificationSource, Double classificationConfidence,
        long reporterId, Long assigneeId, String assignmentStrategy, String awaitingAssignmentReason,
        Sla sla, ResolutionView resolution, Integer feedbackRating, Long duplicateOfId, int reopenCount,
        Instant createdAt, Instant updatedAt, List<NoteView> notes,
        Set<IncidentAction> allowedActions, Set<IncidentStatus> allowedNext) {

    public record Sla(String policy, Instant responseDueAt, Instant resolutionDueAt, Instant respondedAt,
            boolean paused) {
    }

    public record ResolutionView(String summary, String rootCause, Instant resolvedAt, Long resolvedById) {
    }

    public record NoteView(long id, String text, Long authorId, boolean internal, Instant createdAt) {
    }

    public static IncidentView of(Incident i, Actor viewer) {
        SlaDeadlines s = i.sla();
        Resolution r = i.getResolution();
        boolean showInternal = viewer.isSystem() || !viewer.hasAnyRole(Role.EMPLOYEE);
        List<NoteView> notes = i.getNotes().stream()
                .filter(n -> showInternal || !n.isInternal())
                .map(IncidentView::note)
                .toList();
        return new IncidentView(i.getId(), i.getCode(), i.getTitle(), i.getDescription(), i.getStatus(),
                i.getPriority(), i.getPriorityScore(), i.getPriorityRationale(),
                i.getImpact(), i.getUrgency(), i.getAffectedUsers(),
                i.getCategoryCode(), i.getServiceId(),
                i.getClassification() == null ? null : i.getClassification().getSource(),
                i.getClassification() == null ? null : i.getClassification().getConfidence(),
                i.getReporterId(), i.getAssigneeId(), i.getAssignmentStrategy(), i.getAwaitingAssignmentReason(),
                new Sla(s.getPolicyName(), s.getResponseDueAt(), s.getResolutionDueAt(), s.getRespondedAt(),
                        s.isPaused()),
                r == null ? null : new ResolutionView(r.getSummary(), r.getRootCause(), r.getResolvedAt(),
                        r.getResolvedById()),
                i.getFeedback() == null ? null : i.getFeedback().getRating(),
                i.getDuplicateOfId(), i.getReopenCount(), i.getCreatedAt(), i.getUpdatedAt(), notes,
                i.allowedActions(viewer), i.allowedNext());
    }

    private static NoteView note(InvestigationNote n) {
        return new NoteView(n.getId(), n.getText(), n.getAuthorId(), n.isInternal(), n.getCreatedAt());
    }
}
