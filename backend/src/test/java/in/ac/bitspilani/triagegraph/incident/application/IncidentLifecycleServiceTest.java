package in.ac.bitspilani.triagegraph.incident.application;

import in.ac.bitspilani.triagegraph.incident.domain.Actor;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.AssignmentRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ClassificationSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityInput;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.SlaTargets;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;
import in.ac.bitspilani.triagegraph.shared.contracts.events.DomainEvent;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentAssigned;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentReported;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentStatusChanged;
import in.ac.bitspilani.triagegraph.shared.contracts.events.PriorityAssigned;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.AssignmentService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.BlastRadiusProvider;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.DomainEventPublisher;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.PriorityService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.SlaService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.TriageService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.DuplicateIncidentException;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidStateTransitionException;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import in.ac.bitspilani.triagegraph.shared.exception.WorkloadExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Orchestration of report() and the lifecycle commands against mocked ports (no Spring). */
class IncidentLifecycleServiceTest {

    static final Instant NOW = Instant.parse("2026-11-20T09:00:00Z");
    static final long DEV = 5, SNEHA = 20, ARJUN = 21, ANANYA = 30;
    static final Actor DEV_ACTOR = Actor.of(DEV, Role.EMPLOYEE);
    static final Actor SNEHA_ACTOR = Actor.of(SNEHA, Role.TECHNICIAN);
    static final ServiceRef FEE_APP = new ServiceRef(1, "FEE_PAYMENT", "Fee Payment App", Criticality.MISSION_CRITICAL);
    static final ReportIncidentCommand PAYMENT_DOWN = new ReportIncidentCommand("Fee payment app unavailable",
            "Students cannot complete transactions", Impact.EXTENSIVE, Urgency.CRITICAL, 300, null, null);

    final InMemoryIncidentRepository repo = new InMemoryIncidentRepository();
    final TriageService triage = mock(TriageService.class);
    final CatalogQuery catalog = mock(CatalogQuery.class);
    final PriorityService priority = mock(PriorityService.class);
    final BlastRadiusProvider blast = mock(BlastRadiusProvider.class);
    final SlaService sla = mock(SlaService.class);
    final AssignmentService assignment = mock(AssignmentService.class);
    final UserDirectory users = mock(UserDirectory.class);
    final List<DomainEvent> published = new ArrayList<>();
    final DomainEventPublisher events = published::addAll;
    IncidentLifecycleService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        service = new IncidentLifecycleService(repo, new IncidentFactory(clock), triage, catalog, priority, blast,
                sla, assignment, users, events, clock);
        when(users.user(DEV)).thenReturn(new UserRef(DEV, "Dev", Role.EMPLOYEE, "dev@campus", null));
        when(users.user(SNEHA)).thenReturn(new UserRef(SNEHA, "Sneha", Role.TECHNICIAN, "sneha@campus", null));
        when(users.user(ARJUN)).thenReturn(new UserRef(ARJUN, "Arjun", Role.TECHNICIAN, "arjun@campus", null));
        when(users.user(ANANYA)).thenReturn(new UserRef(ANANYA, "Ananya", Role.TEAM_LEAD, "ananya@campus", null));
        when(triage.classify(anyString()))
                .thenReturn(new ClassificationSuggestion("APPLICATION", 1L, 0.9, "KEYWORD", "fee payment"));
        when(catalog.service(1)).thenReturn(FEE_APP);
        when(catalog.category("APPLICATION")).thenReturn(new CategoryRef("APPLICATION", "Application", "APPLICATION"));
        when(blast.dependentCount(1)).thenReturn(0);
        when(priority.assess(any())).thenReturn(new PriorityAssessment(Priority.P1, 83.2, "WEIGHTED", "rationale"));
        when(sla.targetsFor(1, Priority.P1))
                .thenReturn(new SlaTargets(Duration.ofMinutes(3), Duration.ofMinutes(20), 75, "Fee Payment P1"));
        when(assignment.autoAssign(any()))
                .thenReturn(new TechnicianRecommendation(SNEHA, "Sneha", 0.68, "SMART", List.of("skill match")));
    }

    @Test
    void reportClassifiesPrioritisesStartsSlaAndReachesAssigned() {
        IncidentView view = service.report(PAYMENT_DOWN, DEV_ACTOR);

        assertThat(view.status()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(view.code()).isEqualTo("INC-1001");
        assertThat(view.priority()).isEqualTo(Priority.P1);
        assertThat(view.priorityScore()).isEqualTo(83.2);
        assertThat(view.assigneeId()).isEqualTo(SNEHA);
        assertThat(view.assignmentStrategy()).isEqualTo("SMART");
        assertThat(view.classificationSource()).isEqualTo("KEYWORD");
        assertThat(view.sla().responseDueAt()).isEqualTo(NOW.plus(Duration.ofMinutes(3)));
        assertThat(view.sla().resolutionDueAt()).isEqualTo(NOW.plus(Duration.ofMinutes(20)));
        assertThat(view.allowedActions()).containsExactly(IncidentAction.ADD_NOTE);

        ArgumentCaptor<PriorityInput> input = ArgumentCaptor.forClass(PriorityInput.class);
        verify(priority).assess(input.capture());
        assertThat(input.getValue()).isEqualTo(new PriorityInput(Impact.EXTENSIVE, Urgency.CRITICAL,
                Criticality.MISSION_CRITICAL, 300, 0, "APPLICATION"));
        verify(triage).checkNotRepeatSubmission(DEV, PAYMENT_DOWN.text());
        verify(triage).index(1L, PAYMENT_DOWN.text());
        verify(assignment).autoAssign(new AssignmentRequest(1L, "APPLICATION", 1L, Priority.P1));

        assertThat(published).extracting(e -> e.getClass().getSimpleName()).containsExactly(
                "IncidentReported", "IncidentStatusChanged", "PriorityAssigned", "IncidentAssigned");
        assertThat(((IncidentReported) published.getFirst()).serviceId()).isEqualTo(1L);
        assertThat(((IncidentStatusChanged) published.get(1)).to()).isEqualTo(IncidentStatus.CLASSIFIED);
        assertThat(((PriorityAssigned) published.get(2)).actorId()).isNull();
        assertThat(((IncidentAssigned) published.get(3)).technicianId()).isEqualTo(SNEHA);
    }

    @Test
    void theReportersOwnChoiceSkipsTheClassifier() {
        when(catalog.service(3)).thenReturn(new ServiceRef(3, "HOSTEL_WIFI", "Hostel Wi-Fi", Criticality.HIGH));
        when(catalog.category("NETWORK")).thenReturn(new CategoryRef("NETWORK", "Network", "NETWORK"));
        when(sla.targetsFor(3, Priority.P1)).thenReturn(new SlaTargets(Duration.ofMinutes(15), Duration.ofHours(4), 75, "x"));

        IncidentView view = service.report(new ReportIncidentCommand("No Wi-Fi", "Block 5", Impact.SIGNIFICANT,
                Urgency.HIGH, 80, "network", 3L), DEV_ACTOR);

        verify(triage, never()).classify(anyString());
        assertThat(view.categoryCode()).isEqualTo("NETWORK");
        assertThat(view.serviceId()).isEqualTo(3L);
        assertThat(view.classificationSource()).isEqualTo("USER");
    }

    @Test
    void whenNobodyIsFreeTheIncidentStaysClassifiedWithAReason() {
        when(assignment.autoAssign(any())).thenThrow(new WorkloadExceededException("Everyone is at full load"));

        IncidentView view = service.report(PAYMENT_DOWN, DEV_ACTOR);

        assertThat(view.status()).isEqualTo(IncidentStatus.CLASSIFIED);
        assertThat(view.assigneeId()).isNull();
        assertThat(view.awaitingAssignmentReason()).isEqualTo("Everyone is at full load");
        assertThat(published).hasSize(3).noneMatch(IncidentAssigned.class::isInstance);
    }

    @Test
    void aRepeatSubmissionIsRejectedBeforeAnythingIsSaved() {
        org.mockito.Mockito.doThrow(new DuplicateIncidentException("Looks like INC-1001"))
                .when(triage).checkNotRepeatSubmission(anyLong(), anyString());

        assertThatThrownBy(() -> service.report(PAYMENT_DOWN, DEV_ACTOR)).isInstanceOf(DuplicateIncidentException.class);
        assertThat(repo.store).isEmpty();
        assertThat(published).isEmpty();
    }

    @Test
    void anUnclassifiableReportAsksForAService() {
        when(triage.classify(anyString())).thenReturn(new ClassificationSuggestion("APPLICATION", null, 0.1, "FALLBACK", ""));

        assertThatThrownBy(() -> service.report(PAYMENT_DOWN, DEV_ACTOR)).isInstanceOf(InvalidInputException.class);
        assertThat(repo.store).isEmpty();
    }

    @Test
    void autoAssignmentMustPickATechnician() {
        when(assignment.autoAssign(any()))
                .thenReturn(new TechnicianRecommendation(DEV, "Dev", 1, "BROKEN", List.of()));

        assertThatThrownBy(() -> service.report(PAYMENT_DOWN, DEV_ACTOR)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void theAssigneeWalksTheIncidentToResolvedAndTheReporterClosesTheLoop() {
        long id = service.report(PAYMENT_DOWN, DEV_ACTOR).id();
        published.clear();

        service.acknowledge(id, SNEHA_ACTOR);
        service.startWork(id, SNEHA_ACTOR);
        service.requestInfo(id, "Which payment method?", SNEHA_ACTOR);
        service.provideInfo(id, "UPI", DEV_ACTOR);
        service.resolve(id, "Renewed the TLS certificate", "Certificate expired", SNEHA_ACTOR);
        service.verify(id, DEV_ACTOR);
        IncidentView closed = service.close(id, SNEHA_ACTOR);
        IncidentView rated = service.submitFeedback(id, 5, "Fast", DEV_ACTOR);

        assertThat(closed.status()).isEqualTo(IncidentStatus.CLOSED);
        assertThat(closed.allowedActions()).isEmpty();             // Sneha cannot leave feedback
        assertThat(rated.feedbackRating()).isEqualTo(5);
        assertThat(rated.notes()).extracting(IncidentView.NoteView::text).containsExactly("Which payment method?", "UPI");
        assertThat(published).filteredOn(IncidentStatusChanged.class::isInstance).hasSize(7);
    }

    @Test
    void illegalTransitionsPropagateAndPublishNothing() {
        long id = service.report(PAYMENT_DOWN, DEV_ACTOR).id();
        published.clear();
        int saves = repo.saves;

        assertThatThrownBy(() -> service.resolve(id, "Fixed", null, SNEHA_ACTOR))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> service.close(id, SNEHA_ACTOR)).isInstanceOf(InvalidStateTransitionException.class);

        assertThat(published).isEmpty();
        assertThat(repo.saves).isEqualTo(saves);
        assertThat(service.get(id, DEV_ACTOR).status()).isEqualTo(IncidentStatus.ASSIGNED);
    }

    @Test
    void teamLeadReassignsByHand() {
        long id = service.report(PAYMENT_DOWN, DEV_ACTOR).id();

        IncidentView view = service.assign(id, ARJUN, Actor.of(ANANYA, Role.TEAM_LEAD));

        assertThat(view.assigneeId()).isEqualTo(ARJUN);
        assertThat(view.assignmentStrategy()).isEqualTo(IncidentLifecycleService.MANUAL);
        assertThatThrownBy(() -> service.assign(id, DEV, Actor.of(ANANYA, Role.TEAM_LEAD)))
                .isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.assign(id, SNEHA, DEV_ACTOR)).isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void strangersCannotOpenSomeoneElsesIncident() {
        long id = service.report(PAYMENT_DOWN, DEV_ACTOR).id();

        assertThatThrownBy(() -> service.get(id, Actor.of(99, Role.EMPLOYEE)))
                .isInstanceOf(UnauthorizedOperationException.class);
        assertThatThrownBy(() -> service.acknowledge(id, Actor.of(ARJUN, Role.TECHNICIAN)))
                .isInstanceOf(UnauthorizedOperationException.class);
        assertThatThrownBy(() -> service.get(404, DEV_ACTOR)).isInstanceOf(IncidentNotFoundException.class);
    }

    @Test
    void employeesDoNotSeeInternalNotes() {
        long id = service.report(PAYMENT_DOWN, DEV_ACTOR).id();
        service.addNote(id, "Gateway cert expires today", true, SNEHA_ACTOR);
        service.addNote(id, "Looking into it", false, SNEHA_ACTOR);

        assertThat(service.get(id, DEV_ACTOR).notes()).extracting(IncidentView.NoteView::text)
                .containsExactly("Looking into it");
        assertThat(service.get(id, SNEHA_ACTOR).notes()).hasSize(2);
    }

    @Test
    void theSystemCannotReport() {
        assertThatThrownBy(() -> service.report(PAYMENT_DOWN, Actor.system())).isInstanceOf(InvalidInputException.class);
    }
}
