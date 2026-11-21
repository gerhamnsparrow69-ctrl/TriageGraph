package in.ac.bitspilani.triagegraph.assignment.application;

import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.provider;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.sneha;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.unavailable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.TechnicianUnavailableException;
import in.ac.bitspilani.triagegraph.shared.exception.WorkloadExceededException;

class ReassignmentServiceTest {

    private final Fakes.Directory directory = new Fakes.Directory();
    private final Fakes.Commands commands = new Fakes.Commands();

    @Test
    void reassignsThroughTheIncidentCommandPort() {
        directory.technicians.add(sneha(2));
        new ReassignmentService(directory, provider(commands)).reassign(10, 4, " Needs DB skills ", 2, false);
        assertEquals(List.of("reassign 10 to 4 by 2: Needs DB skills"), commands.calls);
    }

    @Test
    void fullTechnicianIsRejectedUnlessAnAdminOverrides() {
        directory.technicians.add(sneha(6));
        var service = new ReassignmentService(directory, provider(commands));
        assertThrows(WorkloadExceededException.class, () -> service.reassign(10, 4, "urgent", 2, false));
        service.reassign(10, 4, "urgent", 1, true);
        assertEquals(List.of("reassign 10 to 4 by 1: [admin override] urgent"), commands.calls);
    }

    @Test
    void unavailableOrUnknownTechnicianAndBlankReasonAreRejected() {
        directory.technicians.add(unavailable(sneha(0)));
        var service = new ReassignmentService(directory, provider(commands));
        assertThrows(TechnicianUnavailableException.class, () -> service.reassign(10, 4, "x", 2, false));
        assertThrows(TechnicianUnavailableException.class, () -> service.reassign(10, 99, "x", 2, false));
        assertThrows(InvalidInputException.class, () -> service.reassign(10, 4, "  ", 2, false));
    }

    @Test
    void withoutTheIncidentModuleTheCallFailsWith503() {
        directory.technicians.add(sneha(0));
        var service = new ReassignmentService(directory, provider(null));
        assertThrows(AssignmentDependencyUnavailableException.class, () -> service.reassign(10, 4, "x", 2, false));
    }
}
