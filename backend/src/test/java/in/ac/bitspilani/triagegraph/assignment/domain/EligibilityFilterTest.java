package in.ac.bitspilani.triagegraph.assignment.domain;

import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.ananya;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.arjun;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.ravi;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.sneha;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.unavailable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.exception.TechnicianUnavailableException;
import in.ac.bitspilani.triagegraph.shared.exception.WorkloadExceededException;

class EligibilityFilterTest {

    private final EligibilityFilter filter = new EligibilityFilter();

    @Test
    void keepsOnlySkilledAvailableTechniciansWithFreeCapacity() {
        List<TechnicianProfile> eligible = filter.eligible("APPLICATION",
                List.of(sneha(1), ananya(4), arjun(0), unavailable(ravi(0))));
        assertEquals(1, eligible.size());
        assertEquals("Sneha Reddy", eligible.get(0).name());
    }

    /** TC12 boundary: at max load (6/6) is excluded, one below (5/6) is still eligible. */
    @Test
    void technicianAtMaxLoadIsExcludedButOneBelowIsEligible() {
        assertEquals(1, filter.eligible("APPLICATION", List.of(sneha(5))).size());
        assertThrows(WorkloadExceededException.class, () -> filter.eligible("APPLICATION", List.of(sneha(6))));
    }

    /** TC12: everyone with the skill is full → WorkloadExceededException (not "nobody has the skill"). */
    @Test
    void everySkilledTechnicianFullThrowsWorkloadExceeded() {
        WorkloadExceededException e = assertThrows(WorkloadExceededException.class,
                () -> filter.eligible("APPLICATION", List.of(sneha(6), ananya(4), arjun(0))));
        assertTrue(e.getMessage().contains("2 available APPLICATION technicians"), e.getMessage());
    }

    @Test
    void nobodyWithTheSkillThrowsTechnicianUnavailable() {
        TechnicianUnavailableException e = assertThrows(TechnicianUnavailableException.class,
                () -> filter.eligible("SECURITY", List.of(sneha(0), arjun(0))));
        assertEquals("No technician has the SECURITY skill.", e.getMessage());
    }

    @Test
    void skilledButAllUnavailableThrowsTechnicianUnavailable() {
        TechnicianUnavailableException e = assertThrows(TechnicianUnavailableException.class,
                () -> filter.eligible("NETWORK", List.of(unavailable(ravi(0)))));
        assertTrue(e.getMessage().contains("unavailable"));
    }

    @Test
    void skillMatchIgnoresCaseAndSpaces() {
        assertEquals(1, filter.eligible("  application ", List.of(sneha(0))).size());
    }
}
