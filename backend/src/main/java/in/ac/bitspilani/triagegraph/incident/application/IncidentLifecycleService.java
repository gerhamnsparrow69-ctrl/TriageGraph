package in.ac.bitspilani.triagegraph.incident.application;

import in.ac.bitspilani.triagegraph.incident.domain.Actor;
import in.ac.bitspilani.triagegraph.incident.domain.ClassificationInfo;
import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.incident.domain.IncidentRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.AssignmentRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ClassificationSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityInput;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.AssignmentService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.BlastRadiusProvider;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.DomainEventPublisher;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.PriorityService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.SlaService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.TriageService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.AssignmentException;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * One entry point for everything that happens to an incident (Facade). Orchestrates the other modules
 * only through the shared ports, so a stub and the real implementation are interchangeable.
 * <p>
 * Every command: load → check the caller may see it → let the aggregate's state decide → save →
 * publish the pending events (Observer). Events go out inside the transaction, so the audit listener
 * (BEFORE_COMMIT) is atomic with the change, and side effects (AFTER_COMMIT) never see a rollback.
 */
@Service
@Transactional
public class IncidentLifecycleService {

    /** Strategy key recorded when a team lead or admin assigns by hand. */
    public static final String MANUAL = "MANUAL";

    private final IncidentRepository repository;
    private final IncidentFactory factory;
    private final TriageService triage;
    private final CatalogQuery catalog;
    private final PriorityService priority;
    private final BlastRadiusProvider blastRadius;
    private final SlaService sla;
    private final AssignmentService assignment;
    private final UserDirectory users;
    private final DomainEventPublisher events;
    private final Clock clock;

    public IncidentLifecycleService(IncidentRepository repository, IncidentFactory factory, TriageService triage,
            CatalogQuery catalog, PriorityService priority, BlastRadiusProvider blastRadius, SlaService sla,
            AssignmentService assignment, UserDirectory users, DomainEventPublisher events, Clock clock) {
        this.repository = repository;
        this.factory = factory;
        this.triage = triage;
        this.catalog = catalog;
        this.priority = priority;
        this.blastRadius = blastRadius;
        this.sla = sla;
        this.assignment = assignment;
        this.users = users;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Report → classify → prioritise → start SLA → auto-assign (blueprint §5). Ends in ASSIGNED, or stays
     * CLASSIFIED with a reason when nobody suitable is free.
     *
     * @throws in.ac.bitspilani.triagegraph.shared.exception.DuplicateIncidentException same reporter, same text
     * @throws in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException   unknown service
     */
    public IncidentView report(ReportIncidentCommand cmd, Actor reporter) {
        if (reporter.isSystem()) {
            throw new InvalidInputException("An incident needs a reporter.");
        }
        users.user(reporter.id());                                            // UserNotFoundException
        triage.checkNotRepeatSubmission(reporter.id(), cmd.text());
        ClassificationSuggestion suggestion = cmd.hasUserChoice() ? cmd.userChoice() : triage.classify(cmd.text());
        if (suggestion.serviceId() == null || suggestion.categoryCode() == null) {
            throw new InvalidInputException("We could not tell which service is affected; please choose one.");
        }
        ServiceRef service = catalog.service(suggestion.serviceId());
        String categoryCode = catalog.category(suggestion.categoryCode()).code();
        Instant now = Instant.now(clock);

        Incident incident = factory.create(cmd, reporter.id());                                   // NEW
        incident.classify(categoryCode, service.id(), ClassificationInfo.from(suggestion), Actor.system(), now);
        incident.applyPriority(priority.assess(new PriorityInput(incident.getImpact(), incident.getUrgency(),
                service.criticality(), incident.getAffectedUsers(), blastRadius.dependentCount(service.id()),
                categoryCode)), Actor.system(), now);
        incident.startSla(sla.targetsFor(service.id(), incident.getPriority()), now);
        incident = repository.save(incident);
        triage.index(incident.getId(), cmd.text());

        try {
            TechnicianRecommendation pick = assignment.autoAssign(new AssignmentRequest(incident.getId(),
                    categoryCode, service.id(), incident.getPriority()));
            requireTechnician(pick.technicianId());
            incident.assignTo(pick.technicianId(), pick.strategyKey(), Actor.system(), now);       // ASSIGNED
        } catch (AssignmentException e) {
            incident.flagAwaitingAssignment(e.getMessage());                  // stays CLASSIFIED
        }
        return saveAndPublish(incident, reporter);
    }

    @Transactional(readOnly = true)
    public IncidentView get(long id, Actor viewer) {
        return IncidentView.of(load(id, viewer), viewer);
    }

    /** Manual (re)assignment by a team lead or admin. */
    public IncidentView assign(long id, long technicianId, Actor by) {
        requireTechnician(technicianId);
        return apply(id, by, (i) -> i.assignTo(technicianId, MANUAL, by, now()));
    }

    public IncidentView acknowledge(long id, Actor by) {
        return apply(id, by, i -> i.acknowledge(by, now()));
    }

    public IncidentView startWork(long id, Actor by) {
        return apply(id, by, i -> i.startWork(by, now()));
    }

    public IncidentView requestInfo(long id, String question, Actor by) {
        return apply(id, by, i -> i.requestInfo(question, by, now()));
    }

    public IncidentView provideInfo(long id, String answer, Actor by) {
        return apply(id, by, i -> i.provideInfo(answer, by, now()));
    }

    public IncidentView resolve(long id, String summary, String rootCause, Actor by) {
        return apply(id, by, i -> i.resolve(summary, rootCause, by, now()));
    }

    public IncidentView verify(long id, Actor by) {
        return apply(id, by, i -> i.verify(by, now()));
    }

    public IncidentView reopen(long id, String reason, Actor by) {
        return apply(id, by, i -> i.reopen(reason, by, now()));
    }

    public IncidentView close(long id, Actor by) {
        return apply(id, by, i -> i.close(by, now()));
    }

    public IncidentView addNote(long id, String text, boolean internal, Actor by) {
        return apply(id, by, i -> i.addNote(text, internal, by, now()));
    }

    public IncidentView submitFeedback(long id, int rating, String comment, Actor by) {
        return apply(id, by, i -> i.submitFeedback(rating, comment, by, now()));
    }

    private IncidentView apply(long id, Actor by, Consumer<Incident> action) {
        Incident incident = load(id, by);
        action.accept(incident);
        return saveAndPublish(incident, by);
    }

    private Incident load(long id, Actor viewer) {
        Incident incident = repository.findById(id)
                .orElseThrow(() -> new IncidentNotFoundException("No incident with id " + id));
        if (!incident.canBeViewedBy(viewer)) {
            throw new UnauthorizedOperationException("You do not have access to " + incident.getCode() + ".");
        }
        return incident;
    }

    private IncidentView saveAndPublish(Incident incident, Actor viewer) {
        Incident saved = repository.save(incident);
        events.publishAll(saved.pullEvents());
        return IncidentView.of(saved, viewer);
    }

    private void requireTechnician(long technicianId) {
        UserRef tech = users.user(technicianId);
        if (tech.role() != Role.TECHNICIAN && tech.role() != Role.TEAM_LEAD) {
            throw new InvalidInputException(tech.name() + " is not a technician.");
        }
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
