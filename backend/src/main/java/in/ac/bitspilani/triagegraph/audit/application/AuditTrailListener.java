package in.ac.bitspilani.triagegraph.audit.application;

import in.ac.bitspilani.triagegraph.audit.domain.AuditAction;
import in.ac.bitspilani.triagegraph.audit.domain.AuditDraft;
import in.ac.bitspilani.triagegraph.audit.domain.AuditRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.events.CatalogChanged;
import in.ac.bitspilani.triagegraph.shared.contracts.events.DomainEvent;
import in.ac.bitspilani.triagegraph.shared.contracts.events.DuplicateLinked;
import in.ac.bitspilani.triagegraph.shared.contracts.events.FeedbackSubmitted;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentAssigned;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentEscalated;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentReported;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentStatusChanged;
import in.ac.bitspilani.triagegraph.shared.contracts.events.MajorIncidentDeclared;
import in.ac.bitspilani.triagegraph.shared.contracts.events.NoteAdded;
import in.ac.bitspilani.triagegraph.shared.contracts.events.PriorityAssigned;
import in.ac.bitspilani.triagegraph.shared.contracts.events.SlaBreached;
import in.ac.bitspilani.triagegraph.shared.contracts.events.SlaWarningRaised;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Locale;

/**
 * Records every domain event in the audit chain (Observer). It runs BEFORE_COMMIT, inside the same
 * transaction as the change, so an incident change and its audit entry are saved together or not at all.
 * Events published outside a transaction are still recorded ({@code fallbackExecution}).
 * <p>
 * The pattern-matching switch is exhaustive over the sealed {@link DomainEvent} hierarchy: adding an
 * event type to the contracts fails compilation here until it is audited.
 */
@Component
public class AuditTrailListener {

    private final AuditChainWriter writer;
    private final AuditRepository repository;
    private final UserDirectory users;

    public AuditTrailListener(AuditChainWriter writer, AuditRepository repository, UserDirectory users) {
        this.writer = writer;
        this.repository = repository;
        this.users = users;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void on(DomainEvent event) {
        AuditDraft draft = toDraft(event);
        if (draft.actorId() != null) {
            draft = draft.withActorRole(roleOf(draft.actorId()));
        }
        writer.append(draft);
    }

    AuditDraft toDraft(DomainEvent event) {
        return switch (event) {
            case IncidentReported e -> incident(e.incidentId(), e.incidentCode(), e, AuditAction.CREATED,
                    null, IncidentStatus.NEW, "Reported" + (e.categoryCode() == null ? ""
                            : " as " + e.categoryCode() + " on service " + e.serviceId()));
            case IncidentStatusChanged e -> incident(e.incidentId(), e.incidentCode(), e, actionFor(e),
                    e.from(), e.to(), e.reason());
            case PriorityAssigned e -> incident(e.incidentId(), e.incidentCode(), e, AuditAction.PRIORITY_SET,
                    null, null, (e.previous() == null ? "" : e.previous() + " → ") + e.current()
                            + String.format(Locale.ROOT, " (score %.1f): %s", e.score(), e.rationale()));
            case IncidentAssigned e -> incident(e.incidentId(), e.incidentCode(), e,
                    e.previousTechnicianId() == null ? AuditAction.ASSIGNED : AuditAction.REASSIGNED,
                    null, IncidentStatus.ASSIGNED, "Assigned to technician " + e.technicianId()
                            + (e.previousTechnicianId() == null ? "" : " (was " + e.previousTechnicianId() + ")")
                            + " by " + e.strategyKey())
                    .withFromState(previousState(e.incidentId(), e.previousTechnicianId()));
            case NoteAdded e -> incident(e.incidentId(), e.incidentCode(), e, AuditAction.NOTE_ADDED, null, null,
                    (e.internal() ? "Internal note " : "Note ") + e.noteId() + " added");
            case SlaWarningRaised e -> incident(e.incidentId(), e.incidentCode(), e, AuditAction.SLA_WARNING,
                    null, null, e.kind() + " SLA at risk, due " + e.dueAt());
            case SlaBreached e -> incident(e.incidentId(), e.incidentCode(), e, AuditAction.SLA_BREACHED,
                    null, null, e.kind() + " SLA breached, was due " + e.dueAt());
            case IncidentEscalated e -> incident(e.incidentId(), e.incidentCode(), e, AuditAction.ESCALATED,
                    null, null, "Escalated to level " + e.level() + " (user " + e.targetUserId() + "): " + e.reason());
            case DuplicateLinked e -> incident(e.incidentId(), e.incidentCode(), e,
                    e.merged() ? AuditAction.DUPLICATE_LINKED : AuditAction.DUPLICATE_JOINED, null, null,
                    (e.merged() ? "Merged as a duplicate of incident " : "User " + e.userId()
                            + " joined as a duplicate of incident ") + e.duplicateOfId());
            case FeedbackSubmitted e -> incident(e.incidentId(), e.incidentCode(), e, AuditAction.FEEDBACK_SUBMITTED,
                    null, null, e.rating() + "★" + (e.comment() == null ? "" : ": " + e.comment()));
            case MajorIncidentDeclared e -> new AuditDraft(e.occurredAt(), e.actorId(), null,
                    AuditAction.MAJOR_DECLARED, null, e.code(), null, null,
                    "Major incident on service " + e.rootCauseServiceId() + " grouping " + e.childIds());
            case CatalogChanged e -> new AuditDraft(e.occurredAt(), e.actorId(), null, AuditAction.CONFIG_CHANGED,
                    null, null, null, null, e.entityType() + " " + e.entityKey() + ": " + e.change());
        };
    }

    /** Status changes get a specific action where the transition has a name of its own. */
    static AuditAction actionFor(IncidentStatusChanged e) {
        return switch (e.to()) {
            case CLASSIFIED -> AuditAction.CLASSIFIED;
            case ACKNOWLEDGED -> AuditAction.ACKNOWLEDGED;
            case PENDING_INFO -> AuditAction.INFO_REQUESTED;
            case IN_PROGRESS -> switch (e.from()) {
                case ACKNOWLEDGED -> AuditAction.WORK_STARTED;
                case PENDING_INFO -> AuditAction.INFO_PROVIDED;
                case RESOLVED -> AuditAction.REOPENED;
                default -> AuditAction.STATUS_CHANGED;
            };
            case RESOLVED -> AuditAction.RESOLVED;
            case VERIFIED -> AuditAction.VERIFIED;
            case CLOSED -> AuditAction.CLOSED;
            case MERGED -> AuditAction.MERGED;
            default -> AuditAction.STATUS_CHANGED;
        };
    }

    /**
     * IncidentAssigned is the move into ASSIGNED but does not say where from: a first assignment comes
     * from CLASSIFIED; a reassignment from whatever the trail last recorded (ASSIGNED or ACKNOWLEDGED).
     */
    private IncidentStatus previousState(long incidentId, Long previousTechnicianId) {
        return repository.lastKnownState(incidentId)
                .orElse(previousTechnicianId == null ? IncidentStatus.CLASSIFIED : null);
    }

    private static AuditDraft incident(long incidentId, String code, DomainEvent e, AuditAction action,
            IncidentStatus from, IncidentStatus to, String description) {
        return new AuditDraft(e.occurredAt(), e.actorId(), null, action, incidentId, code, from, to, description);
    }

    /** Best effort: an unknown actor must never stop the change being recorded (or roll it back). */
    private Role roleOf(long actorId) {
        try {
            UserRef user = users.user(actorId);
            return user == null ? null : user.role();
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }
}
