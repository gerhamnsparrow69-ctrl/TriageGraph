package in.ac.bitspilani.triagegraph.incident.domain;

import in.ac.bitspilani.triagegraph.incident.domain.state.IncidentState;
import in.ac.bitspilani.triagegraph.incident.domain.state.IncidentStateRegistry;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.SlaTargets;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;
import in.ac.bitspilani.triagegraph.shared.contracts.events.DuplicateLinked;
import in.ac.bitspilani.triagegraph.shared.contracts.events.FeedbackSubmitted;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentAssigned;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentEvent;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentReported;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentStatusChanged;
import in.ac.bitspilani.triagegraph.shared.contracts.events.NoteAdded;
import in.ac.bitspilani.triagegraph.shared.contracts.events.PriorityAssigned;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidStateTransitionException;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * The incident aggregate. Built by {@code IncidentFactory} through {@link #builder()}, always in NEW.
 * <p>
 * There are no public setters for status, priority or assignee. Every lifecycle action is delegated to
 * the {@link IncidentState} for the current status (State pattern), which rejects illegal actions with
 * {@link InvalidStateTransitionException} (409) and otherwise calls the "state hooks" further down.
 * Those hooks re-check the transition table, so even they cannot move the incident illegally.
 * <p>
 * Other modules are referenced by id only (reporter, assignee, service, category): the incident module
 * talks to them through the contracts in {@code shared}, never through their entities.
 * <p>
 * Changes are recorded as pending domain events and published by the lifecycle service after saving
 * (Observer). Events are built when pulled, so they carry the generated id and code.
 * A full NEW → CLOSED run produces nine events: IncidentReported, StatusChanged(→CLASSIFIED),
 * PriorityAssigned, IncidentAssigned (this is the move into ASSIGNED, so no separate StatusChanged),
 * then StatusChanged for ACKNOWLEDGED, IN_PROGRESS, RESOLVED, VERIFIED and CLOSED.
 */
@Entity
@Table(name = "incidents")
public class Incident implements Trackable {

    public static final int MAX_TITLE = 200;
    public static final int MAX_DESCRIPTION = 4000;
    static final long CODE_OFFSET = 1000;
    private static final String CODE_PREFIX = "INC-";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    @Column(nullable = false, length = MAX_TITLE)
    private String title;

    @Column(nullable = false, length = MAX_DESCRIPTION)
    private String description;

    @Column(nullable = false)
    private Long reporterId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "incident_followers", joinColumns = @JoinColumn(name = "incident_id"))
    @Column(name = "user_id")
    private Set<Long> followerIds = new HashSet<>();

    @Column(length = 50)
    private String categoryCode;

    private Long serviceId;

    @Embedded
    private ClassificationInfo classification;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Impact impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Urgency urgency;

    private int affectedUsers;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private Priority priority;

    private Double priorityScore;

    @Column(length = 1000)
    private String priorityRationale;

    @Column(length = 40)
    private String priorityCalculator;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    private Long assigneeId;

    @Column(length = 40)
    private String assignmentStrategy;

    /** Why auto-assignment failed; the incident stays CLASSIFIED until a team lead assigns it. */
    @Column(length = 500)
    private String awaitingAssignmentReason;

    private int escalationLevel;

    private int reopenCount;

    @Embedded
    private SlaDeadlines sla = new SlaDeadlines();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "incident_id")
    @OrderBy("createdAt ASC, id ASC")
    private List<InvestigationNote> notes = new ArrayList<>();

    @Embedded
    private Resolution resolution;

    @Embedded
    private Closure closure;

    @Embedded
    private Feedback feedback;

    private Long duplicateOfId;

    private Long majorIncidentId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Transient
    private final List<Function<Incident, IncidentEvent>> pendingEvents = new ArrayList<>();

    protected Incident() {
    }

    private Incident(Builder b) {
        this.title = requireText(b.title, "Title", MAX_TITLE);
        this.description = requireText(b.description, "Description", MAX_DESCRIPTION);
        this.reporterId = Objects.requireNonNull(b.reporterId, "reporterId");
        this.impact = Objects.requireNonNull(b.impact, "impact");
        this.urgency = Objects.requireNonNull(b.urgency, "urgency");
        if (b.affectedUsers < 1) {
            throw new InvalidInputException("Affected users must be at least 1.");
        }
        this.affectedUsers = b.affectedUsers;
        this.createdAt = Objects.requireNonNull(b.createdAt, "createdAt");
        this.updatedAt = b.createdAt;
        this.status = IncidentStatus.NEW;
        Long actorId = reporterId;
        Instant at = createdAt;
        // Built at pull time: by then the incident is classified, so the event carries service and category.
        raise(i -> new IncidentReported(i.id, i.getCode(), actorId, at,
                i.serviceId == null ? 0 : i.serviceId, i.categoryCode));
    }

    public static Builder builder() {
        return new Builder();
    }

    // ------------------------------------------------------------------ lifecycle (delegates to state)

    public void classify(String categoryCode, long serviceId, ClassificationInfo info, Actor by, Instant now) {
        state().classify(this, categoryCode, serviceId, info, by, now);
    }

    /** First assignment (CLASSIFIED) or reassignment (ASSIGNED, ACKNOWLEDGED). */
    public void assignTo(long technicianId, String strategyKey, Actor by, Instant now) {
        state().assign(this, technicianId, strategyKey, by, now);
    }

    public void acknowledge(Actor by, Instant now) {
        state().acknowledge(this, by, now);
    }

    public void startWork(Actor by, Instant now) {
        state().startWork(this, by, now);
    }

    public void requestInfo(String question, Actor by, Instant now) {
        state().requestInfo(this, question, by, now);
    }

    public void provideInfo(String answer, Actor by, Instant now) {
        state().provideInfo(this, answer, by, now);
    }

    public void resolve(String summary, String rootCause, Actor by, Instant now) {
        state().resolve(this, summary, rootCause, by, now);
    }

    public void verify(Actor by, Instant now) {
        state().verify(this, by, now);
    }

    public void reopen(String reason, Actor by, Instant now) {
        state().reopen(this, reason, by, now);
    }

    public void close(Actor by, Instant now) {
        state().close(this, by, now);
    }

    public void mergeInto(Incident parent, Actor by, Instant now) {
        state().mergeInto(this, parent, by, now);
    }

    public void submitFeedback(int rating, String comment, Actor by, Instant now) {
        state().submitFeedback(this, rating, comment, by, now);
    }

    // ------------------------------------------------------------------ non-transition behaviour

    /** Records the priority from {@code PriorityService}. Allowed once classified and until closed. */
    public void applyPriority(PriorityAssessment assessment, Actor by, Instant now) {
        if (status == IncidentStatus.NEW || isTerminal()) {
            throw new InvalidStateTransitionException(
                    "Cannot set the priority of an incident that is " + status + ".");
        }
        Priority previous = priority;
        this.priority = assessment.priority();
        this.priorityScore = assessment.score();
        this.priorityRationale = assessment.rationale();
        this.priorityCalculator = assessment.calculatorKey();
        this.updatedAt = now;
        raise(i -> new PriorityAssigned(i.id, i.getCode(), by.id(), now, previous, assessment.priority(),
                assessment.score(), assessment.rationale()));
    }

    /** Starts the response and resolution clocks from the SLA targets that apply to this incident. */
    public void startSla(SlaTargets targets, Instant now) {
        if (isTerminal()) {
            throw new InvalidStateTransitionException("Cannot start the SLA of an incident that is " + status + ".");
        }
        sla().start(targets, now);
    }

    /** Auto-assignment found nobody; the incident stays CLASSIFIED and waits for a team lead. */
    public void flagAwaitingAssignment(String reason) {
        if (status != IncidentStatus.CLASSIFIED) {
            throw new InvalidStateTransitionException(
                    "Only a CLASSIFIED incident can wait for assignment; this one is " + status + ".");
        }
        this.awaitingAssignmentReason = reason;
    }

    public InvestigationNote addNote(String text, boolean internal, Actor by, Instant now) {
        if (isTerminal()) {
            throw new InvalidStateTransitionException("Cannot add a note to an incident that is " + status + ".");
        }
        requirePermitted(IncidentAction.ADD_NOTE, by);
        if (internal && by.hasAnyRole(Role.EMPLOYEE)) {
            throw new UnauthorizedOperationException("Only IT staff can add internal notes.");
        }
        return appendNote(text, internal, by, now);
    }

    /** A user who reported the same problem joins this incident and is kept informed. */
    public void addFollower(long userId) {
        if (userId != reporterId) {
            followerIds.add(userId);
        }
    }

    // ------------------------------------------------------------------ permissions

    /** Object-level access (IDOR protection): who may open this incident at all. */
    public boolean canBeViewedBy(Actor by) {
        if (by.isSystem() || by.hasAnyRole(Role.TEAM_LEAD, Role.ADMIN, Role.IT_MANAGER)) {
            return true;
        }
        if (isReporter(by) || (by.id() != null && followerIds.contains(by.id()))) {
            return true;
        }
        return by.hasAnyRole(Role.TECHNICIAN) && isAssignee(by);
    }

    /** Whether {@code by} is the kind of person who may perform {@code action}; state rules are separate. */
    public boolean permits(IncidentAction action, Actor by) {
        return switch (action) {
            case CLASSIFY, MERGE -> by.isSystem() || (by.role() != null && by.role() != Role.EMPLOYEE);
            case ASSIGN, ESCALATE -> by.isSystem() || by.hasAnyRole(Role.TEAM_LEAD, Role.ADMIN, Role.IT_MANAGER);
            case ACKNOWLEDGE, START_WORK, REQUEST_INFO, RESOLVE -> isAssignee(by);
            case PROVIDE_INFO, REOPEN, FEEDBACK -> isReporter(by);
            case VERIFY -> by.isSystem() || isReporter(by);
            case CLOSE -> by.isSystem() || isAssignee(by)
                    || by.hasAnyRole(Role.TEAM_LEAD, Role.ADMIN, Role.IT_MANAGER);
            case ADD_NOTE -> canBeViewedBy(by);
        };
    }

    /** What {@code by} can do right now: the UI renders exactly these buttons. */
    public Set<IncidentAction> allowedActions(Actor by) {
        EnumSet<IncidentAction> result = EnumSet.noneOf(IncidentAction.class);
        for (IncidentAction a : state().allowedActions()) {
            if (permits(a, by) && !(a == IncidentAction.FEEDBACK && feedback != null)) {
                result.add(a);
            }
        }
        if (!isTerminal() && permits(IncidentAction.ADD_NOTE, by)) {
            result.add(IncidentAction.ADD_NOTE);
        }
        return result;
    }

    public Set<IncidentStatus> allowedNext() {
        return state().allowedNext();
    }

    // ------------------------------------------------------------------ state hooks (IncidentState only)

    /** Throws 403 unless {@code by} may perform {@code action}. Called by states after the state check. */
    public void requirePermitted(IncidentAction action, Actor by) {
        if (!permits(action, by)) {
            throw new UnauthorizedOperationException(
                    "You are not allowed to " + describe(action) + " " + label() + ".");
        }
    }

    /** Moves to {@code to} (must be in the current state's allowedNext) and records the status change. */
    public void transitionTo(IncidentStatus to, Actor by, Instant now, String reason) {
        IncidentStatus from = status;
        moveTo(to, now);
        raise(i -> new IncidentStatusChanged(i.id, i.getCode(), by.id(), now, from, to, reason));
    }

    /** Sets the assignee and moves to ASSIGNED; the IncidentAssigned event is the status change. */
    public void changeAssignee(long technicianId, String strategyKey, Actor by, Instant now) {
        if (assigneeId != null && assigneeId == technicianId) {
            throw new InvalidInputException(label() + " is already assigned to this technician.");
        }
        Long previous = assigneeId;
        moveTo(IncidentStatus.ASSIGNED, now);
        this.assigneeId = technicianId;
        this.assignmentStrategy = strategyKey;
        this.awaitingAssignmentReason = null;
        raise(i -> new IncidentAssigned(i.id, i.getCode(), by.id(), now, previous, technicianId, strategyKey));
    }

    public void recordClassification(String categoryCode, long serviceId, ClassificationInfo info) {
        this.categoryCode = requireText(categoryCode, "Category", 50);
        this.serviceId = serviceId;
        this.classification = info;
    }

    public void recordResolution(String summary, String rootCause, Actor by, Instant now) {
        this.resolution = new Resolution(requireText(summary, "Resolution summary", 2000),
                rootCause == null || rootCause.isBlank() ? null : rootCause.strip(), now, by.id());
    }

    /** Reopen discards the rejected resolution and counts the reopen. */
    public void discardResolution() {
        this.resolution = null;
        this.reopenCount++;
    }

    public void recordClosure(Actor by, Instant now) {
        this.closure = new Closure(now, by.id());
    }

    public void recordFeedback(int rating, String comment, Actor by, Instant now) {
        this.feedback = new Feedback(rating, comment == null || comment.isBlank() ? null : comment.strip(), now);
        this.updatedAt = now;
        raise(i -> new FeedbackSubmitted(i.id, i.getCode(), by.id(), now, rating, i.feedback.getComment()));
    }

    /** Links this incident to {@code parent} as a duplicate; the reporter starts following the parent. */
    public void linkAsDuplicateOf(Incident parent, Actor by, Instant now) {
        if (parent == this || parent.getId() == null || parent.getId().equals(id)) {
            throw new InvalidInputException("An incident cannot be merged into itself.");
        }
        if (parent.isTerminal()) {
            throw new InvalidStateTransitionException(
                    "Cannot merge into " + parent.label() + " because it is " + parent.getStatus() + ".");
        }
        this.duplicateOfId = parent.getId();
        parent.addFollower(reporterId);
        long parentId = parent.getId();
        long reporter = reporterId;
        raise(i -> new DuplicateLinked(i.id, i.getCode(), by.id(), now, parentId, reporter, true));
    }

    /** Adds a note without the permission check; states use it to record questions and answers. */
    public InvestigationNote appendNote(String text, boolean internal, Actor by, Instant now) {
        InvestigationNote note = new InvestigationNote(requireText(text, "Note", InvestigationNote.MAX_LENGTH),
                by.id(), internal, now);
        notes.add(note);
        this.updatedAt = now;
        // note.getId() is read at pull time, after the repository has saved and flushed.
        raise(i -> new NoteAdded(i.id, i.getCode(), by.id(), now, note.getId(), internal));
        return note;
    }

    public SlaDeadlines sla() {
        if (sla == null) {           // Hibernate leaves an all-null embeddable as null
            sla = new SlaDeadlines();
        }
        return sla;
    }

    public boolean isAssignee(Actor by) {
        return by.is(assigneeId);
    }

    public boolean isReporter(Actor by) {
        return by.is(reporterId);
    }

    /** The code ("INC-1001"), or "this incident" before it is saved: used in messages. */
    public String label() {
        return id == null ? "this incident" : getCode();
    }

    // ------------------------------------------------------------------ events and snapshots

    /** Returns and clears the pending events. The incident must have been saved (it needs an id). */
    public List<IncidentEvent> pullEvents() {
        if (id == null) {
            throw new IllegalStateException("Save the incident before pulling its events.");
        }
        List<IncidentEvent> events = pendingEvents.stream().map(f -> f.apply(this)).toList();
        pendingEvents.clear();
        return events;
    }

    public boolean hasPendingEvents() {
        return !pendingEvents.isEmpty();
    }

    public IncidentSnapshot toSnapshot() {
        return new IncidentSnapshot(id, getCode(), title, description, status, priority,
                serviceId == null ? 0 : serviceId, categoryCode, reporterId, Set.copyOf(followerIds),
                assigneeId, majorIncidentId, createdAt, sla().getResponseDueAt(), sla().getResolutionDueAt());
    }

    public boolean isTerminal() {
        return state().isTerminal();
    }

    public static Long idFromCode(String code) {
        if (code == null || !code.toUpperCase().startsWith(CODE_PREFIX)) {
            throw new InvalidInputException("Incident codes look like INC-1001.");
        }
        try {
            return Long.parseLong(code.substring(CODE_PREFIX.length())) - CODE_OFFSET;
        } catch (NumberFormatException e) {
            throw new InvalidInputException("Incident codes look like INC-1001.", e);
        }
    }

    private IncidentState state() {
        return IncidentStateRegistry.of(status);
    }

    private void moveTo(IncidentStatus to, Instant now) {
        if (!state().allowedNext().contains(to)) {
            throw new InvalidStateTransitionException(
                    "Cannot move " + label() + " from " + status + " to " + to + ".");
        }
        this.status = to;
        this.updatedAt = now;
    }

    private void raise(Function<Incident, IncidentEvent> event) {
        pendingEvents.add(event);
    }

    private static String describe(IncidentAction action) {
        return action.name().toLowerCase().replace('_', ' ');
    }

    public static String requireText(String value, String what, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidInputException(what + " must not be blank.");
        }
        String stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new InvalidInputException(what + " must be at most " + maxLength + " characters.");
        }
        return stripped;
    }

    // ------------------------------------------------------------------ getters

    @Override
    public Long getId() {
        return id;
    }

    /** Human-friendly code derived from the id: id 1 → "INC-1001". */
    @Override
    public String getCode() {
        return id == null ? null : CODE_PREFIX + (CODE_OFFSET + id);
    }

    @Override
    public IncidentStatus getStatus() {
        return status;
    }

    @Override
    public Priority getPriority() {
        return priority;
    }

    public long getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public Set<Long> getFollowerIds() {
        return Collections.unmodifiableSet(followerIds);
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public ClassificationInfo getClassification() {
        return classification;
    }

    public Impact getImpact() {
        return impact;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public int getAffectedUsers() {
        return affectedUsers;
    }

    public Double getPriorityScore() {
        return priorityScore;
    }

    public String getPriorityRationale() {
        return priorityRationale;
    }

    public String getPriorityCalculator() {
        return priorityCalculator;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public String getAssignmentStrategy() {
        return assignmentStrategy;
    }

    public String getAwaitingAssignmentReason() {
        return awaitingAssignmentReason;
    }

    public int getEscalationLevel() {
        return escalationLevel;
    }

    public int getReopenCount() {
        return reopenCount;
    }

    public List<InvestigationNote> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    public Resolution getResolution() {
        return resolution;
    }

    public Closure getClosure() {
        return closure;
    }

    public Feedback getFeedback() {
        return feedback;
    }

    public Long getDuplicateOfId() {
        return duplicateOfId;
    }

    public Long getMajorIncidentId() {
        return majorIncidentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // ------------------------------------------------------------------ builder

    /** Builder for the many fields of a new incident. {@link #build()} validates and always starts in NEW. */
    public static final class Builder {

        private String title;
        private String description;
        private Long reporterId;
        private Impact impact;
        private Urgency urgency;
        private int affectedUsers = 1;
        private Instant createdAt;

        private Builder() {
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder reporterId(long reporterId) {
            this.reporterId = reporterId;
            return this;
        }

        public Builder impact(Impact impact) {
            this.impact = impact;
            return this;
        }

        public Builder urgency(Urgency urgency) {
            this.urgency = urgency;
            return this;
        }

        public Builder affectedUsers(int affectedUsers) {
            this.affectedUsers = affectedUsers;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Incident build() {
            return new Incident(this);
        }
    }
}
