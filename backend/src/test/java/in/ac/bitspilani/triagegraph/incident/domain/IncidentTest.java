package in.ac.bitspilani.triagegraph.incident.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;
import in.ac.bitspilani.triagegraph.shared.contracts.events.DuplicateLinked;
import in.ac.bitspilani.triagegraph.shared.contracts.events.FeedbackSubmitted;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentAssigned;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentEvent;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentReported;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentStatusChanged;
import in.ac.bitspilani.triagegraph.shared.contracts.events.PriorityAssigned;
import in.ac.bitspilani.triagegraph.shared.exception.FeedbackNotAllowedException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidStateTransitionException;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static in.ac.bitspilani.triagegraph.incident.domain.IncidentFixtures.*;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction.*;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class IncidentTest {

    @Test
    void builderStartsInNewWithAReportedEvent() {
        Incident i = newIncident();

        assertThat(i.getStatus()).isEqualTo(NEW);
        assertThat(i.getCreatedAt()).isEqualTo(T0);
        assertThat(pull(i)).singleElement().isInstanceOf(IncidentReported.class);
    }

    @Test
    void builderRejectsBadInput() {
        assertThatThrownBy(() -> Incident.builder().title(" ").description("d").reporterId(1)
                .impact(Impact.MINOR).urgency(Urgency.LOW).createdAt(T0).build())
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("Title");
        assertThatThrownBy(() -> Incident.builder().title("t").description("x".repeat(4001)).reporterId(1)
                .impact(Impact.MINOR).urgency(Urgency.LOW).createdAt(T0).build())
                .isInstanceOf(InvalidInputException.class).hasMessageContaining("4000");
        assertThatThrownBy(() -> Incident.builder().title("t").description("d").reporterId(1)
                .impact(Impact.MINOR).urgency(Urgency.LOW).affectedUsers(0).createdAt(T0).build())
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void codeIsDerivedFromTheId() {
        Incident i = newIncident();
        giveId(i, 24);

        assertThat(i.getCode()).isEqualTo("INC-1024");
        assertThat(Incident.idFromCode("inc-1024")).isEqualTo(24L);
        assertThatThrownBy(() -> Incident.idFromCode("1024")).isInstanceOf(InvalidInputException.class);
    }

    /** The event stream that TC05's audit trail will be built from: nine events, in order. */
    @Test
    void fullLifecycleFromNewToClosedRaisesNineOrderedEvents() {
        Incident i = newIncident();

        i.classify("APPLICATION", 1, new ClassificationInfo("KEYWORD", 0.9, "fee"), Actor.system(), T0);
        i.applyPriority(new PriorityAssessment(Priority.P1, 83.2, "WEIGHTED", "4×4×4×1.30×1.00"), Actor.system(), T0);
        i.startSla(P1_SLA, T0);
        i.assignTo(TECH_ID, "SMART", Actor.system(), T0);
        i.acknowledge(TECH, T0.plusSeconds(60));
        i.startWork(TECH, T0.plusSeconds(120));
        i.resolve("Renewed the TLS certificate", "Certificate expired", TECH, T0.plusSeconds(900));
        i.verify(REPORTER, T0.plusSeconds(1000));
        i.close(TECH, T0.plusSeconds(1100));

        List<IncidentEvent> events = i.pullEvents();
        assertThat(events).extracting(e -> e.getClass().getSimpleName()).containsExactly(
                "IncidentReported", "IncidentStatusChanged", "PriorityAssigned", "IncidentAssigned",
                "IncidentStatusChanged", "IncidentStatusChanged", "IncidentStatusChanged",
                "IncidentStatusChanged", "IncidentStatusChanged");
        assertThat(events).filteredOn(IncidentStatusChanged.class::isInstance)
                .map(IncidentStatusChanged.class::cast)
                .extracting(IncidentStatusChanged::from, IncidentStatusChanged::to)
                .containsExactly(tuple(NEW, CLASSIFIED), tuple(ASSIGNED, ACKNOWLEDGED),
                        tuple(ACKNOWLEDGED, IN_PROGRESS), tuple(IN_PROGRESS, RESOLVED),
                        tuple(RESOLVED, VERIFIED), tuple(VERIFIED, CLOSED));
        IncidentReported reported = (IncidentReported) events.getFirst();
        assertThat(reported.serviceId()).isEqualTo(1);
        assertThat(reported.categoryCode()).isEqualTo("APPLICATION");
        assertThat(reported.incidentCode()).isEqualTo(i.getCode());
        assertThat(((PriorityAssigned) events.get(2)).score()).isEqualTo(83.2);
        assertThat(((IncidentAssigned) events.get(3)).previousTechnicianId()).isNull();
        assertThat(i.getStatus()).isEqualTo(CLOSED);
        assertThat(i.getClosure().getClosedById()).isEqualTo(TECH_ID);
        assertThat(i.sla().getRespondedAt()).isEqualTo(T0.plusSeconds(60));
        assertThat(i.pullEvents()).as("pulling clears the events").isEmpty();
    }

    /** TC16: close() on a NEW incident is a 409 and changes nothing. */
    @Test
    void closingANewIncidentIsAnInvalidTransition() {
        Incident i = newIncident();
        i.pullEvents();

        assertThatThrownBy(() -> i.close(ADMIN, T0))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessage("Cannot close an incident that is NEW. Allowed now: classify, merge.");
        assertThat(i.getStatus()).isEqualTo(NEW);
        assertThat(i.hasPendingEvents()).isFalse();
    }

    /** TC17: resolve() while ASSIGNED (not acknowledged) is a 409, even for the assignee. */
    @Test
    void resolvingBeforeAcknowledgingIsAnInvalidTransition() {
        Incident i = inStatus(ASSIGNED);

        assertThatThrownBy(() -> i.resolve("Fixed", null, TECH, T0))
                .isInstanceOf(InvalidStateTransitionException.class);
        assertThat(i.getStatus()).isEqualTo(ASSIGNED);
        assertThat(i.getResolution()).isNull();
        assertThat(i.hasPendingEvents()).isFalse();
    }

    @Test
    void stateIsCheckedBeforeWhoIsAsking() {
        Incident i = inStatus(NEW);

        assertThatThrownBy(() -> i.acknowledge(OTHER_EMPLOYEE, T0)).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void onlyTheAssigneeCanWorkTheIncident() {
        Incident i = inStatus(ASSIGNED);

        assertThatThrownBy(() -> i.acknowledge(OTHER_TECH, T0)).isInstanceOf(UnauthorizedOperationException.class);
        assertThatThrownBy(() -> i.acknowledge(LEAD, T0)).isInstanceOf(UnauthorizedOperationException.class);
        assertThat(i.getStatus()).isEqualTo(ASSIGNED);
    }

    @Test
    void onlyTheReporterCanVerifyOrReopen() {
        Incident i = inStatus(RESOLVED);

        assertThatThrownBy(() -> i.verify(TECH, T0)).isInstanceOf(UnauthorizedOperationException.class);
        assertThatThrownBy(() -> i.reopen("no", OTHER_EMPLOYEE, T0)).isInstanceOf(UnauthorizedOperationException.class);
        i.verify(REPORTER, T0);
        assertThat(i.getStatus()).isEqualTo(VERIFIED);
    }

    @Test
    void employeesAndTechniciansCannotReassign() {
        Incident i = inStatus(ASSIGNED);

        assertThatThrownBy(() -> i.assignTo(OTHER_TECH_ID, "MANUAL", REPORTER, T0))
                .isInstanceOf(UnauthorizedOperationException.class);
        assertThatThrownBy(() -> i.assignTo(OTHER_TECH_ID, "MANUAL", TECH, T0))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void reassigningAnAcknowledgedIncidentSendsItBackToAssigned() {
        Incident i = inStatus(ACKNOWLEDGED);

        i.assignTo(OTHER_TECH_ID, "MANUAL", LEAD, T0.plusSeconds(300));

        assertThat(i.getStatus()).isEqualTo(ASSIGNED);
        assertThat(i.getAssigneeId()).isEqualTo(OTHER_TECH_ID);
        IncidentAssigned e = (IncidentAssigned) i.pullEvents().getFirst();
        assertThat(e.previousTechnicianId()).isEqualTo(TECH_ID);
        assertThat(e.actorId()).isEqualTo(LEAD_ID);
        assertThat(e.strategyKey()).isEqualTo("MANUAL");
    }

    @Test
    void reassigningToTheSameTechnicianIsRejected() {
        Incident i = inStatus(ASSIGNED);

        assertThatThrownBy(() -> i.assignTo(TECH_ID, "MANUAL", LEAD, T0)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void waitingForTheReporterPausesTheResolutionClock() {
        Incident i = inStatus(IN_PROGRESS);
        var dueBefore = i.sla().getResolutionDueAt();

        i.requestInfo("Which browser?", TECH, T0.plusSeconds(1000));
        assertThat(i.sla().isPaused()).isTrue();
        i.provideInfo("Chrome", REPORTER, T0.plusSeconds(1000 + 1800));

        assertThat(i.getStatus()).isEqualTo(IN_PROGRESS);
        assertThat(i.sla().isPaused()).isFalse();
        assertThat(i.sla().getResolutionDueAt()).isEqualTo(dueBefore.plus(Duration.ofMinutes(30)));
        assertThat(i.getNotes()).extracting(InvestigationNote::getText).containsExactly("Which browser?", "Chrome");
    }

    @Test
    void reopenNeedsAReasonAndDiscardsTheResolution() {
        Incident i = inStatus(RESOLVED);

        assertThatThrownBy(() -> i.reopen(" ", REPORTER, T0)).isInstanceOf(InvalidInputException.class);
        assertThat(i.getStatus()).isEqualTo(RESOLVED);

        i.reopen("Still failing on mobile", REPORTER, T0.plusSeconds(900));

        assertThat(i.getStatus()).isEqualTo(IN_PROGRESS);
        assertThat(i.getResolution()).isNull();
        assertThat(i.getReopenCount()).isEqualTo(1);
        assertThat(i.sla().isPaused()).isFalse();
        assertThat(((IncidentStatusChanged) i.pullEvents().getFirst()).reason()).contains("Still failing on mobile");
    }

    @Test
    void mergingMakesTheReporterFollowTheParent() {
        Incident parent = inStatus(ASSIGNED);
        Incident dup = newIncident();
        dup.pullEvents();

        dup.mergeInto(parent, LEAD, T0);

        assertThat(dup.getStatus()).isEqualTo(MERGED);
        assertThat(dup.getDuplicateOfId()).isEqualTo(parent.getId());
        assertThat(parent.getFollowerIds()).isEmpty();          // same reporter: not added as a follower
        List<IncidentEvent> events = dup.pullEvents();
        assertThat(events.getFirst()).isInstanceOf(DuplicateLinked.class);
        assertThat(((DuplicateLinked) events.getFirst()).duplicateOfId()).isEqualTo(parent.getId());
    }

    @Test
    void cannotMergeIntoAClosedIncidentOrItself() {
        Incident closed = inStatus(CLOSED);
        Incident dup = inStatus(NEW);

        assertThatThrownBy(() -> dup.mergeInto(closed, LEAD, T0)).isInstanceOf(InvalidStateTransitionException.class);
        assertThatThrownBy(() -> dup.mergeInto(dup, LEAD, T0)).isInstanceOf(InvalidInputException.class);
        assertThat(dup.getStatus()).isEqualTo(NEW);
    }

    @Test
    void feedbackOnceByTheReporterWithAValidRating() {
        Incident i = inStatus(CLOSED);

        assertThatThrownBy(() -> i.submitFeedback(6, null, REPORTER, T0)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> i.submitFeedback(5, null, TECH, T0)).isInstanceOf(UnauthorizedOperationException.class);
        i.submitFeedback(5, " Quick fix ", REPORTER, T0);

        assertThat(((FeedbackSubmitted) i.pullEvents().getFirst()).comment()).isEqualTo("Quick fix");
        assertThat(i.allowedActions(REPORTER)).isEmpty();
        assertThatThrownBy(() -> i.submitFeedback(4, null, REPORTER, T0)).isInstanceOf(FeedbackNotAllowedException.class);
    }

    @Test
    void allowedActionsDependOnStateAndCaller() {
        Incident i = inStatus(ASSIGNED);

        assertThat(i.allowedActions(TECH)).containsExactlyInAnyOrder(ACKNOWLEDGE, ADD_NOTE);
        assertThat(i.allowedActions(LEAD)).containsExactlyInAnyOrder(ASSIGN, ADD_NOTE);
        assertThat(i.allowedActions(REPORTER)).containsExactly(ADD_NOTE);
        assertThat(i.allowedActions(OTHER_EMPLOYEE)).isEmpty();
        assertThat(i.allowedNext()).containsExactlyInAnyOrder(ACKNOWLEDGED, ASSIGNED);
    }

    @Test
    void viewAccessIsObjectLevel() {
        Incident i = inStatus(ASSIGNED);

        assertThat(i.canBeViewedBy(REPORTER)).isTrue();
        assertThat(i.canBeViewedBy(TECH)).isTrue();
        assertThat(i.canBeViewedBy(LEAD)).isTrue();
        assertThat(i.canBeViewedBy(ADMIN)).isTrue();
        assertThat(i.canBeViewedBy(OTHER_EMPLOYEE)).isFalse();
        assertThat(i.canBeViewedBy(OTHER_TECH)).isFalse();
        i.addFollower(OTHER_EMPLOYEE.id());
        assertThat(i.canBeViewedBy(OTHER_EMPLOYEE)).isTrue();
    }

    @Test
    void notesAreBlockedOnClosedIncidentsAndInternalNotesAreStaffOnly() {
        Incident open = inStatus(IN_PROGRESS);
        assertThatThrownBy(() -> open.addNote("secret", true, REPORTER, T0))
                .isInstanceOf(UnauthorizedOperationException.class);
        open.addNote("Checked gateway logs", true, TECH, T0);
        assertThat(open.getNotes()).singleElement().matches(InvestigationNote::isInternal);

        Incident closed = inStatus(CLOSED);
        assertThatThrownBy(() -> closed.addNote("late", false, REPORTER, T0))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void priorityNeedsAClassifiedOpenIncident() {
        PriorityAssessment p2 = new PriorityAssessment(Priority.P2, 30, "WEIGHTED", "x");

        assertThatThrownBy(() -> inStatus(NEW).applyPriority(p2, Actor.system(), T0))
                .isInstanceOf(InvalidStateTransitionException.class);
        Incident i = inStatus(IN_PROGRESS);
        i.applyPriority(p2, LEAD, T0);
        PriorityAssigned e = (PriorityAssigned) i.pullEvents().getFirst();
        assertThat(e.previous()).isEqualTo(Priority.P1);
        assertThat(e.current()).isEqualTo(Priority.P2);
    }

    @Test
    void awaitingAssignmentOnlyWhileClassified() {
        Incident i = inStatus(CLASSIFIED);
        i.flagAwaitingAssignment("Nobody free");
        assertThat(i.getAwaitingAssignmentReason()).isEqualTo("Nobody free");

        i.assignTo(TECH_ID, "MANUAL", LEAD, T0);
        assertThat(i.getAwaitingAssignmentReason()).isNull();
        assertThatThrownBy(() -> i.flagAwaitingAssignment("x")).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void eventsNeedASavedIncident() {
        Incident unsaved = Incident.builder().title("t").description("d").reporterId(1)
                .impact(Impact.MINOR).urgency(Urgency.LOW).createdAt(T0).build();

        assertThatThrownBy(unsaved::pullEvents).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void snapshotCarriesTheContractFields() {
        Incident i = inStatus(ASSIGNED);

        var s = i.toSnapshot();
        assertThat(s.status()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(s.assigneeId()).isEqualTo(TECH_ID);
        assertThat(s.reporterId()).isEqualTo(REPORTER_ID);
        assertThat(s.responseDueAt()).isEqualTo(T0.plus(P1_SLA.response()));
        assertThat(s.priority()).isEqualTo(Priority.P1);
    }
}
