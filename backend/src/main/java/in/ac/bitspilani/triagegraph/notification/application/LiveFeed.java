package in.ac.bitspilani.triagegraph.notification.application;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

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

/** Streams every domain event to admins, IT managers and team leads for the Command Centre feed. */
@Component
public class LiveFeed {

    public static final String EVENT = "feed";
    public static final int HISTORY = 50;
    static final Set<Role> WATCHERS = EnumSet.of(Role.ADMIN, Role.IT_MANAGER, Role.TEAM_LEAD);

    private static final Logger log = LoggerFactory.getLogger(LiveFeed.class);

    private final LivePush push;
    private final Deque<FeedItem> recent = new ArrayDeque<>(HISTORY);

    public LiveFeed(LivePush push) {
        this.push = push;
    }

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void on(DomainEvent event) {
        try {
            publish(toItem(event));
        } catch (RuntimeException e) {
            log.warn("Live feed update failed: {}", e.getMessage());
        }
    }

    public void publish(FeedItem item) {
        synchronized (recent) {
            if (recent.size() == HISTORY) {
                recent.removeLast();
            }
            recent.addFirst(item);
        }
        push.toRoles(WATCHERS, EVENT, item);
    }

    /** The last {@value #HISTORY} items, newest first, so the Command Centre isn't empty on page load. */
    public List<FeedItem> recent() {
        synchronized (recent) {
            return List.copyOf(recent);
        }
    }

    static FeedItem toItem(DomainEvent event) {
        return switch (event) {
            case IncidentReported e -> new FeedItem("REPORTED", e.incidentCode(), "New incident reported", e.occurredAt());
            case IncidentAssigned e -> new FeedItem("ASSIGNED", e.incidentCode(),
                    "Assigned to technician #" + e.technicianId() + " (" + e.strategyKey() + ")", e.occurredAt());
            case PriorityAssigned e -> new FeedItem("PRIORITY", e.incidentCode(),
                    "Priority " + e.current() + " (score " + Math.round(e.score() * 10) / 10.0 + ")", e.occurredAt());
            case IncidentStatusChanged e -> new FeedItem("STATUS", e.incidentCode(), e.from() + " → " + e.to(), e.occurredAt());
            case SlaWarningRaised e -> new FeedItem("SLA_WARNING", e.incidentCode(), e.kind() + " SLA at risk", e.occurredAt());
            case SlaBreached e -> new FeedItem("SLA_BREACHED", e.incidentCode(), e.kind() + " SLA breached", e.occurredAt());
            case IncidentEscalated e -> new FeedItem("ESCALATED", e.incidentCode(), "Escalated to level " + e.level(), e.occurredAt());
            case NoteAdded e -> new FeedItem("NOTE", e.incidentCode(), e.internal() ? "Internal note added" : "Note added", e.occurredAt());
            case DuplicateLinked e -> new FeedItem("DUPLICATE", e.incidentCode(),
                    (e.merged() ? "Merged into #" : "Joined #") + e.duplicateOfId(), e.occurredAt());
            case FeedbackSubmitted e -> new FeedItem("FEEDBACK", e.incidentCode(), e.rating() + "★ feedback", e.occurredAt());
            case MajorIncidentDeclared e -> new FeedItem("MAJOR", e.code(),
                    "Major incident declared over " + e.childIds().size() + " incidents", e.occurredAt());
            case CatalogChanged e -> new FeedItem("CATALOG", null, e.entityType() + " " + e.entityKey() + " " + e.change(), e.occurredAt());
        };
    }
}
