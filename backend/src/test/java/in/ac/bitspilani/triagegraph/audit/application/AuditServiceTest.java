package in.ac.bitspilani.triagegraph.audit.application;

import in.ac.bitspilani.triagegraph.audit.domain.AuditAction;
import in.ac.bitspilani.triagegraph.audit.domain.AuditDraft;
import in.ac.bitspilani.triagegraph.audit.domain.AuditEntry;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditServiceTest {

    static final Instant T = Instant.parse("2026-11-20T09:00:00Z");

    final InMemoryAuditRepository repo = new InMemoryAuditRepository();
    final AuditChainWriter writer = new AuditChainWriter(repo);
    final IncidentQueryPort incidents = mock(IncidentQueryPort.class);
    final AuditService service = new AuditService(repo, incidents);

    @Test
    void anEmptyOrIntactChainVerifies() {
        assertThat(service.verify().message()).isEqualTo("0 entries verified ✓");
        write(1201);   // more than two verification batches

        AuditVerification v = service.verify();

        assertThat(v.valid()).isTrue();
        assertThat(v.entriesChecked()).isEqualTo(1201);
        assertThat(v.message()).isEqualTo("1201 entries verified ✓");
    }

    /** TC32 (audit part): one tampered row is reported by id. */
    @Test
    void anEditedRowIsReportedByItsId() {
        write(10);
        AuditEntry victim = repo.rows.get(6);
        ReflectionTestUtils.setField(victim, "description", "Closed by the admin");

        AuditVerification v = service.verify();

        assertThat(v.valid()).isFalse();
        assertThat(v.firstBrokenId()).isEqualTo(victim.getId());
        assertThat(v.entriesChecked()).isEqualTo(6);
        assertThat(v.reason()).contains("changed");
    }

    @Test
    void aDeletedRowBreaksTheLinkOfTheNextOne() {
        write(5);
        AuditEntry next = repo.rows.get(3);
        repo.rows.remove(2);

        AuditVerification v = service.verify();

        assertThat(v.firstBrokenId()).isEqualTo(next.getId());
        assertThat(v.reason()).contains("removed");
    }

    @Test
    void aRewrittenRowWithAFreshHashStillBreaksTheChain() {
        write(5);
        AuditEntry victim = repo.rows.get(1);
        ReflectionTestUtils.setField(victim, "description", "forged");
        ReflectionTestUtils.setField(victim, "hash", victim.computeHash());   // attacker recomputes its hash

        assertThat(service.verify().firstBrokenId()).isEqualTo(repo.rows.get(2).getId());
    }

    @Test
    void timelineIsForPeopleWhoMayOpenTheIncident() {
        when(incidents.find(1)).thenReturn(Optional.of(snapshot()));
        write(3);

        assertThat(service.timeline(1, 5, Role.EMPLOYEE)).hasSize(3);      // reporter
        assertThat(service.timeline(1, 6, Role.EMPLOYEE)).hasSize(3);      // follower
        assertThat(service.timeline(1, 20, Role.TECHNICIAN)).hasSize(3);   // assignee
        assertThat(service.timeline(1, 99, Role.ADMIN)).hasSize(3);
        assertThatThrownBy(() -> service.timeline(1, 7, Role.EMPLOYEE)).isInstanceOf(UnauthorizedOperationException.class);
        assertThatThrownBy(() -> service.timeline(1, 21, Role.TECHNICIAN)).isInstanceOf(UnauthorizedOperationException.class);
        assertThatThrownBy(() -> service.timeline(2, 99, Role.ADMIN)).isInstanceOf(IncidentNotFoundException.class);
    }

    @Test
    void recentIsNewestFirstAndBounded() {
        write(5);

        assertThat(service.recent(0, 2)).extracting(AuditEntryView::id).containsExactly(5L, 4L);
        assertThatThrownBy(() -> service.recent(0, 1000)).isInstanceOf(InvalidInputException.class);
    }

    private void write(int n) {
        for (int i = 0; i < n; i++) {
            writer.append(new AuditDraft(T.plusSeconds(i), 5L, Role.EMPLOYEE, AuditAction.NOTE_ADDED, 1L, "INC-1001",
                    null, null, "Note " + i));
        }
    }

    private static IncidentSnapshot snapshot() {
        return new IncidentSnapshot(1, "INC-1001", "t", "d", IncidentStatus.ASSIGNED, Priority.P1, 1, "APPLICATION",
                5, Set.of(6L), 20L, null, T, T, T);
    }
}
