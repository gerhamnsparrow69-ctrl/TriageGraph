package in.ac.bitspilani.triagegraph.triage.application;

import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.NOW;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.provider;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentCommandPort;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidStateTransitionException;

class JoinMergeServiceTest {

    static final class Commands implements IncidentCommandPort {
        final List<String> calls = new ArrayList<>();

        public void markSlaWarning(long id, SlaKind k) {
        }

        public void markSlaBreached(long id, SlaKind k) {
        }

        public void escalate(long id, int level, long target, String reason) {
        }

        public void reassign(long id, long tech, String reason, long actor) {
        }

        public void joinAsDuplicate(long id, long user) {
            calls.add("join " + id + " " + user);
        }

        public void merge(long child, long parent, long actor) {
            calls.add("merge " + child + "->" + parent + " by " + actor);
        }
    }

    private final Commands commands = new Commands();
    private final TriageTestRig.FakeIncidents incidents = new TriageTestRig.FakeIncidents();
    private final JoinMergeService service = new JoinMergeService(provider(commands), provider(incidents));

    private void incident(long id, IncidentStatus status, long reporter, Set<Long> followers) {
        incidents.replace(new IncidentSnapshot(id, "INC-" + id, "t", "d", status, Priority.P2, 1L, "APPLICATION",
                reporter, followers, 4L, null, NOW, null, null));
    }

    /** TC29 (triage side): joining an open incident hands the user to Member 1's command port. */
    @Test
    void joiningAnOpenIncidentCallsTheCommandPort() {
        incident(1024, IncidentStatus.ASSIGNED, 21, Set.of());
        service.join(1024, 22);
        assertEquals(List.of("join 1024 22"), commands.calls);
    }

    @Test
    void joiningAClosedIncidentOrOneYouAlreadyFollowIsRejected() {
        incident(1, IncidentStatus.CLOSED, 21, Set.of());
        incident(2, IncidentStatus.NEW, 21, Set.of(22L));
        var e = assertThrows(InvalidStateTransitionException.class, () -> service.join(1, 30));
        assertEquals("Cannot join INC-1: it is already CLOSED. Please report a new incident instead.", e.getMessage());
        assertThrows(InvalidInputException.class, () -> service.join(2, 22));
        assertThrows(InvalidInputException.class, () -> service.join(2, 21));
        assertThrows(IncidentNotFoundException.class, () -> service.join(99, 21));
    }

    @Test
    void mergeChecksBothIncidentsAndRefusesSelfMerge() {
        incident(10, IncidentStatus.NEW, 21, Set.of());
        incident(11, IncidentStatus.ASSIGNED, 22, Set.of());
        incident(12, IncidentStatus.MERGED, 23, Set.of());
        service.merge(10, 11, 4);
        assertEquals(List.of("merge 10->11 by 4"), commands.calls);
        assertThrows(InvalidInputException.class, () -> service.merge(10, 10, 4));
        assertThrows(InvalidStateTransitionException.class, () -> service.merge(10, 12, 4));
    }

    @Test
    void withoutTheIncidentModuleJoinFailsWith503() {
        var noModule = new JoinMergeService(provider(null), provider(null));
        assertThrows(TriageDependencyUnavailableException.class, () -> noModule.join(1, 2));
    }
}
