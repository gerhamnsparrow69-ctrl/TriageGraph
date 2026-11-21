package in.ac.bitspilani.triagegraph.assignment.application;

import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.ananya;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.arjun;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.provider;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.ravi;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.sneha;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.exception.UserNotFoundException;

class WorkloadServiceTest {

    private static IncidentSnapshot incident(long id, IncidentStatus status, Priority p, long assignee, String dueAt) {
        return new IncidentSnapshot(id, "INC-" + id, "Incident " + id, "", status, p, 1L, "APPLICATION", 9L, Set.of(),
                assignee, null, Instant.parse("2026-11-21T09:00:00Z"), null, dueAt == null ? null : Instant.parse(dueAt));
    }

    @Test
    void queueIsSortedNeedsActionFirstThenPriorityThenDeadline() {
        var directory = new Fakes.Directory();
        directory.technicians.add(sneha(4));
        var incidents = new Fakes.Incidents();
        incidents.open.addAll(List.of(
                incident(1, IncidentStatus.IN_PROGRESS, Priority.P3, 4, "2026-11-21T12:00:00Z"),
                incident(2, IncidentStatus.PENDING_INFO, Priority.P1, 4, "2026-11-21T10:30:00Z"),
                incident(3, IncidentStatus.ASSIGNED, Priority.P1, 4, "2026-11-21T10:20:00Z"),
                incident(4, IncidentStatus.ACKNOWLEDGED, Priority.P3, 4, "2026-11-21T11:00:00Z"),
                incident(5, IncidentStatus.CLOSED, Priority.P1, 4, null),
                incident(6, IncidentStatus.ASSIGNED, Priority.P2, 3, null)));
        var w = new WorkloadService(directory, provider(incidents)).forTechnician(4);
        assertEquals(List.of("INC-3", "INC-4", "INC-1", "INC-2"), w.queue().stream().map(q -> q.code()).toList());
        assertEquals(67, w.loadPercent());
        assertTrue(w.incidentDataAvailable());
    }

    @Test
    void teamLeadSeesOwnTeamAndThemselvesBusiestFirst() {
        var directory = new Fakes.Directory();
        directory.technicians.addAll(List.of(sneha(1), arjun(6), ananya(2), ravi(3)));
        directory.leadOf.put(4L, 2L);
        directory.leadOf.put(5L, 2L);
        var team = new WorkloadService(directory, provider(null)).team(2);
        assertEquals(List.of("Arjun Mehta", "Ananya Iyer", "Sneha Reddy"), team.stream().map(t -> t.name()).toList());
        assertFalse(team.get(0).atCapacity());
        assertFalse(team.get(0).incidentDataAvailable());
    }

    @Test
    void unknownTechnicianIsNotFound() {
        var service = new WorkloadService(new Fakes.Directory(), provider(null));
        assertThrows(UserNotFoundException.class, () -> service.forTechnician(42));
    }
}
