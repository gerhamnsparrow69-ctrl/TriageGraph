package in.ac.bitspilani.triagegraph.audit.application;

import in.ac.bitspilani.triagegraph.audit.domain.AuditAction;
import in.ac.bitspilani.triagegraph.audit.domain.AuditDraft;
import in.ac.bitspilani.triagegraph.audit.domain.AuditEntry;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
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
import in.ac.bitspilani.triagegraph.shared.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.List;

import static in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditTrailListenerTest {

    static final Instant T = Instant.parse("2026-11-20T09:00:00Z");
    static final long INC = 1;
    static final String CODE = "INC-1001";

    final InMemoryAuditRepository repo = new InMemoryAuditRepository();
    final UserDirectory users = mock(UserDirectory.class);
    final AuditTrailListener listener = new AuditTrailListener(new AuditChainWriter(repo), repo, users);

    @Test
    void recordsEveryEventWithTheActorsRole() {
        when(users.user(5)).thenReturn(new UserRef(5, "Dev", Role.EMPLOYEE, "dev@campus", null));

        listener.on(new IncidentReported(INC, CODE, 5L, T, 1, "APPLICATION"));

        AuditEntry e = repo.rows.getFirst();
        assertThat(e.getAction()).isEqualTo(AuditAction.CREATED);
        assertThat(e.getToState()).isEqualTo(NEW);
        assertThat(e.getActorRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(e.getIncidentCode()).isEqualTo(CODE);
        assertThat(e.getDescription()).isEqualTo("Reported as APPLICATION on service 1");
    }

    @Test
    void unknownActorsAndSystemActionsStillGetRecorded() {
        when(users.user(99)).thenThrow(new UserNotFoundException("gone"));

        listener.on(new NoteAdded(INC, CODE, 99L, T, 3, true));
        listener.on(new SlaBreached(INC, CODE, null, T, SlaKind.RESPONSE, T));

        assertThat(repo.rows).extracting(AuditEntry::getActorRole).containsOnlyNulls();
        assertThat(repo.rows).hasSize(2);
    }

    @ParameterizedTest(name = "{0} → {1}: {2}")
    @CsvSource({
            "NEW, CLASSIFIED, CLASSIFIED",
            "ASSIGNED, ACKNOWLEDGED, ACKNOWLEDGED",
            "ACKNOWLEDGED, IN_PROGRESS, WORK_STARTED",
            "IN_PROGRESS, PENDING_INFO, INFO_REQUESTED",
            "PENDING_INFO, IN_PROGRESS, INFO_PROVIDED",
            "IN_PROGRESS, RESOLVED, RESOLVED",
            "RESOLVED, IN_PROGRESS, REOPENED",
            "RESOLVED, VERIFIED, VERIFIED",
            "VERIFIED, CLOSED, CLOSED",
            "NEW, MERGED, MERGED"})
    void statusChangesGetNamedActions(IncidentStatus from, IncidentStatus to, AuditAction action) {
        AuditDraft d = listener.toDraft(new IncidentStatusChanged(INC, CODE, 5L, T, from, to, "why"));

        assertThat(d.action()).isEqualTo(action);
        assertThat(d.fromState()).isEqualTo(from);
        assertThat(d.toState()).isEqualTo(to);
        assertThat(d.description()).isEqualTo("why");
    }

    @Test
    void assignmentTakesItsFromStateFromTheTrail() {
        listener.on(new IncidentStatusChanged(INC, CODE, null, T, NEW, CLASSIFIED, "x"));
        listener.on(new IncidentAssigned(INC, CODE, null, T, null, 20, "SMART"));
        listener.on(new IncidentStatusChanged(INC, CODE, 20L, T, ASSIGNED, ACKNOWLEDGED, "ack"));
        listener.on(new IncidentAssigned(INC, CODE, 30L, T, 20L, 21, "MANUAL"));

        AuditEntry first = repo.rows.get(1);
        AuditEntry reassign = repo.rows.get(3);
        assertThat(first.getAction()).isEqualTo(AuditAction.ASSIGNED);
        assertThat(first.getFromState()).isEqualTo(CLASSIFIED);
        assertThat(first.getToState()).isEqualTo(ASSIGNED);
        assertThat(reassign.getAction()).isEqualTo(AuditAction.REASSIGNED);
        assertThat(reassign.getFromState()).isEqualTo(ACKNOWLEDGED);
        assertThat(reassign.getDescription()).isEqualTo("Assigned to technician 21 (was 20) by MANUAL");
    }

    @Test
    void firstAssignmentWithNoTrailIsFromClassified() {
        listener.on(new IncidentAssigned(42, "INC-1042", null, T, null, 20, "SMART"));
        assertThat(repo.rows.getFirst().getFromState()).isEqualTo(CLASSIFIED);
    }

    @Test
    void everyOtherEventTypeIsMapped() {
        List<DomainEvent> events = List.of(
                new PriorityAssigned(INC, CODE, null, T, null, Priority.P1, 83.2, "4×4×4×1.30"),
                new SlaWarningRaised(INC, CODE, null, T, SlaKind.RESOLUTION, T),
                new IncidentEscalated(INC, CODE, null, T, 2, 30, "Unacknowledged P1"),
                new DuplicateLinked(INC, CODE, 7L, T, 2, 7, false),
                new DuplicateLinked(INC, CODE, 7L, T, 2, 7, true),
                new FeedbackSubmitted(INC, CODE, 5L, T, 5, "Fast"),
                new MajorIncidentDeclared(9, "MI-9", null, T, 2, List.of(1L, 2L)),
                new CatalogChanged(40L, T, "SERVICE", "ERP", "criticality HIGH → MISSION_CRITICAL"));

        assertThat(events.stream().map(listener::toDraft).map(AuditDraft::action)).containsExactly(
                AuditAction.PRIORITY_SET, AuditAction.SLA_WARNING, AuditAction.ESCALATED,
                AuditAction.DUPLICATE_JOINED, AuditAction.DUPLICATE_LINKED, AuditAction.FEEDBACK_SUBMITTED,
                AuditAction.MAJOR_DECLARED, AuditAction.CONFIG_CHANGED);
        assertThat(listener.toDraft(events.getFirst()).description()).isEqualTo("P1 (score 83.2): 4×4×4×1.30");
        assertThat(listener.toDraft(events.get(6)).incidentId()).isNull();
    }
}
