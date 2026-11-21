package in.ac.bitspilani.triagegraph.notification.application;

import static in.ac.bitspilani.triagegraph.notification.application.NotificationEvents.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.notification.domain.NotificationTrigger;
import in.ac.bitspilani.triagegraph.notification.domain.Recipient;
import in.ac.bitspilani.triagegraph.notification.domain.RecipientRole;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
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
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;

/**
 * Works out who hears about each domain event. Incident details (reporter, followers, priority)
 * come from Member 1's {@link IncidentQueryPort}; without it only ids carried by the event are used.
 */
@Component
public class RecipientResolver {

    private final UserDirectory users;
    private final ObjectProvider<IncidentQueryPort> incidents;

    public RecipientResolver(UserDirectory users, ObjectProvider<IncidentQueryPort> incidents) {
        this.users = users;
        this.incidents = incidents;
    }

    public Optional<NotificationTrigger> resolve(DomainEvent event) {
        return switch (event) {
            case IncidentReported e -> about(e.incidentId(), e.incidentCode(), REPORTED, null, b -> b.reporter());
            case IncidentAssigned e -> about(e.incidentId(), e.incidentCode(), ASSIGNED, null,
                    b -> b.user(e.technicianId(), RecipientRole.ASSIGNEE).reporter());
            case PriorityAssigned e -> e.previous() == null || e.previous() == e.current()
                    ? Optional.empty()
                    : about(e.incidentId(), e.incidentCode(), PRIORITY_CHANGED,
                            "Priority changed from " + e.previous() + " to " + e.current() + ".",
                            b -> b.assignee().reporter());
            case IncidentStatusChanged e -> statusChange(e);
            case SlaWarningRaised e -> about(e.incidentId(), e.incidentCode(), SLA_WARNING,
                    e.kind().name().toLowerCase() + " deadline at " + e.dueAt() + ".", b -> b.assignee());
            case SlaBreached e -> about(e.incidentId(), e.incidentCode(), SLA_BREACHED,
                    e.kind().name().toLowerCase() + " deadline passed at " + e.dueAt() + ".",
                    b -> b.assignee().teamLeadOfAssignee());
            case IncidentEscalated e -> about(e.incidentId(), e.incidentCode(), ESCALATED,
                    "Escalated to level " + e.level() + ": " + e.reason(),
                    b -> b.user(e.targetUserId(), RecipientRole.ESCALATION_TARGET));
            case MajorIncidentDeclared e -> majorIncident(e);
            case NoteAdded e -> Optional.empty();
            case DuplicateLinked e -> Optional.empty();
            case FeedbackSubmitted e -> Optional.empty();
            case CatalogChanged e -> Optional.empty();
        };
    }

    private Optional<NotificationTrigger> statusChange(IncidentStatusChanged e) {
        return switch (e.to()) {
            case PENDING_INFO -> about(e.incidentId(), e.incidentCode(), INFO_REQUESTED, e.reason(), b -> b.reporter());
            case RESOLVED -> about(e.incidentId(), e.incidentCode(), RESOLVED, e.reason(), b -> b.reporter().followers());
            case CLOSED -> about(e.incidentId(), e.incidentCode(), CLOSED, e.reason(), b -> b.reporter());
            default -> Optional.empty();
        };
    }

    private Optional<NotificationTrigger> majorIncident(MajorIncidentDeclared e) {
        Builder b = new Builder(null);
        users.withRole(Role.IT_MANAGER).forEach(u -> b.add(u.id(), RecipientRole.IT_MANAGER));
        users.withRole(Role.ADMIN).forEach(u -> b.add(u.id(), RecipientRole.ADMIN));
        users.withRole(Role.TEAM_LEAD).forEach(u -> b.add(u.id(), RecipientRole.TEAM_LEAD));
        IncidentQueryPort port = incidents.getIfAvailable();
        if (port != null) {
            e.childIds().forEach(id -> port.find(id).ifPresent(s -> b.add(s.reporterId(), RecipientRole.AFFECTED_REPORTER)));
        }
        return Optional.of(new NotificationTrigger(MAJOR_DECLARED, null, e.majorIncidentId(), e.code(), null,
                e.childIds().size() + " related incidents grouped under " + e.code() + ".", b.recipients()));
    }

    private Optional<NotificationTrigger> about(long incidentId, String code, String eventType, String detail,
            Consumer<Builder> who) {
        IncidentSnapshot snapshot = Optional.ofNullable(incidents.getIfAvailable())
                .flatMap(p -> p.find(incidentId)).orElse(null);
        Builder b = new Builder(snapshot);
        who.accept(b);
        if (b.recipients().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new NotificationTrigger(eventType, snapshot == null ? null : snapshot.priority(),
                incidentId, code, snapshot == null ? null : snapshot.title(), detail, b.recipients()));
    }

    /** Collects recipients, keeping each user once (first role wins). */
    private final class Builder {

        private final IncidentSnapshot incident;
        private final Map<Long, RecipientRole> roles = new LinkedHashMap<>();

        Builder(IncidentSnapshot incident) {
            this.incident = incident;
        }

        Builder add(long userId, RecipientRole role) {
            roles.putIfAbsent(userId, role);
            return this;
        }

        Builder user(long userId, RecipientRole role) {
            return add(userId, role);
        }

        Builder reporter() {
            if (incident != null) {
                add(incident.reporterId(), RecipientRole.REPORTER);
            }
            return this;
        }

        Builder assignee() {
            if (incident != null && incident.assigneeId() != null) {
                add(incident.assigneeId(), RecipientRole.ASSIGNEE);
            }
            return this;
        }

        Builder followers() {
            if (incident != null && incident.followerIds() != null) {
                incident.followerIds().forEach(id -> add(id, RecipientRole.FOLLOWER));
            }
            return this;
        }

        Builder teamLeadOfAssignee() {
            if (incident != null && incident.assigneeId() != null) {
                users.teamLeadOf(incident.assigneeId()).ifPresent(l -> add(l.id(), RecipientRole.TEAM_LEAD));
            }
            return this;
        }

        List<Recipient> recipients() {
            List<Recipient> out = new ArrayList<>();
            roles.forEach((id, role) -> out.add(new Recipient(users.user(id), role)));
            return out;
        }
    }
}
