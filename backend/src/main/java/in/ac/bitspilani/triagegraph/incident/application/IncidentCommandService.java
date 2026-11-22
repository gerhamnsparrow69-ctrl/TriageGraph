package in.ac.bitspilani.triagegraph.incident.application;

import in.ac.bitspilani.triagegraph.incident.domain.Actor;
import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.incident.domain.IncidentRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityInput;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.BlastRadiusProvider;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.DomainEventPublisher;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentCommandPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.PriorityService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Commands other modules send to incidents through the shared port (triage: join and merge; assignment:
 * reassign; SLA: warnings, breaches, escalation). The lifecycle rules stay in the aggregate and its states;
 * this class only loads, applies, saves and publishes, like {@link IncidentLifecycleService}.
 */
@Service
@Transactional
public class IncidentCommandService implements IncidentCommandPort {

    private static final Logger log = LoggerFactory.getLogger(IncidentCommandService.class);

    private final IncidentRepository repository;
    private final IncidentLifecycleService lifecycle;
    private final CatalogQuery catalog;
    private final PriorityService priority;
    private final BlastRadiusProvider blastRadius;
    private final UserDirectory users;
    private final DomainEventPublisher events;
    private final Clock clock;

    public IncidentCommandService(IncidentRepository repository, IncidentLifecycleService lifecycle,
            CatalogQuery catalog, PriorityService priority, BlastRadiusProvider blastRadius, UserDirectory users,
            DomainEventPublisher events, Clock clock) {
        this.repository = repository;
        this.lifecycle = lifecycle;
        this.catalog = catalog;
        this.priority = priority;
        this.blastRadius = blastRadius;
        this.users = users;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Workflow 1b: the user follows the incident instead of opening a duplicate, affected users goes up by
     * one, and the priority is re-assessed with the new count (it can rise, e.g. P2 → P1).
     */
    @Override
    public void joinAsDuplicate(long incidentId, long userId) {
        Incident incident = load(incidentId);
        Actor joiner = actor(userId);
        Instant now = Instant.now(clock);
        if (!incident.joinAsAffected(userId, joiner, now)) {
            return;
        }
        if (incident.getServiceId() != null && incident.getCategoryCode() != null) {
            ServiceRef service = catalog.service(incident.getServiceId());
            incident.applyPriority(priority.assess(new PriorityInput(incident.getImpact(), incident.getUrgency(),
                    service.criticality(), incident.getAffectedUsers(), blastRadius.dependentCount(service.id()),
                    incident.getCategoryCode())), Actor.system(), now);
        }
        save(incident);
    }

    /** The child becomes MERGED into the parent; its reporter starts following the parent. */
    @Override
    public void merge(long childId, long parentId, long actorId) {
        Incident child = load(childId);
        Incident parent = load(parentId);
        child.mergeInto(parent, actor(actorId), Instant.now(clock));
        save(parent);
        save(child);
    }

    /** Manual reassignment; the reason (e.g. an admin override) is kept as an internal note. */
    @Override
    public void reassign(long incidentId, long technicianId, String reason, long actorId) {
        Actor by = actor(actorId);
        lifecycle.assign(incidentId, technicianId, by);
        if (reason != null && !reason.isBlank()) {
            lifecycle.addNote(incidentId, "Reassigned: " + reason.strip(), true, by);
        }
    }

    // SLA warnings, breaches and escalation come from the SLA module's monitor (a system actor). The aggregate
    // records them and raises SlaWarningRaised / SlaBreached / IncidentEscalated, published on save.

    @Override
    public void markSlaWarning(long incidentId, SlaKind kind) {
        Incident incident = load(incidentId);
        incident.markSlaWarning(kind, Instant.now(clock));
        save(incident);
        log.info("SLA {} warning recorded for {}", kind, incident.getCode());
    }

    @Override
    public void markSlaBreached(long incidentId, SlaKind kind) {
        Incident incident = load(incidentId);
        incident.markSlaBreached(kind, Instant.now(clock));
        save(incident);
        log.info("SLA {} breach recorded for {}", kind, incident.getCode());
    }

    @Override
    public void escalate(long incidentId, int level, long targetUserId, String reason) {
        Incident incident = load(incidentId);
        incident.escalate(level, targetUserId, reason, Instant.now(clock));
        save(incident);
        log.info("{} escalated to level {} (user {})", incident.getCode(), level, targetUserId);
    }

    private Incident load(long id) {
        return repository.findById(id).orElseThrow(() -> new IncidentNotFoundException("No incident with id " + id));
    }

    private Actor actor(long userId) {
        return Actor.of(userId, users.user(userId).role());
    }

    private void save(Incident incident) {
        Incident saved = repository.save(incident);
        events.publishAll(saved.pullEvents());
    }
}
