package in.ac.bitspilani.triagegraph.assignment.domain;

import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.context;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.tech;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

class RoundRobinStrategyTest {

    private final RoundRobinStrategy strategy = new RoundRobinStrategy();
    private final TechnicianProfile a = tech(1, "A", Set.of("NETWORK"), 0, 6, 1);
    private final TechnicianProfile b = tech(2, "B", Set.of("NETWORK"), 0, 6, 1);
    private final TechnicianProfile c = tech(3, "C", Set.of("NETWORK"), 0, 6, 1);

    /** TC04: three eligible technicians and four assignments → A, B, C, A. */
    @Test
    void fourAssignmentsCycleThroughThreeTechnicians() {
        var ctx = context("NETWORK", 13, Priority.P3);
        List<String> picks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            var top = strategy.rank(ctx, List.of(c, a, b)).get(0);
            strategy.onAssigned(ctx, top.technicianId());
            picks.add(top.name());
        }
        assertEquals(List.of("A", "B", "C", "A"), picks);
    }

    @Test
    void rankingAloneNeverMovesTheCursor() {
        var ctx = context("NETWORK", 13, Priority.P3);
        strategy.rank(ctx, List.of(a, b, c));
        strategy.rank(ctx, List.of(a, b, c));
        assertEquals("A", strategy.rank(ctx, List.of(a, b, c)).get(0).name());
    }

    @Test
    void eachCategoryHasItsOwnTurn() {
        var network = context("NETWORK", 13, Priority.P3);
        var auth = context("AUTHENTICATION", 3, Priority.P3);
        strategy.onAssigned(network, 1);
        assertEquals("B", strategy.rank(network, List.of(a, b, c)).get(0).name());
        assertEquals("A", strategy.rank(auth, List.of(a, b, c)).get(0).name());
    }

    @Test
    void skipsSomeoneWhoLeftTheRotationAndWrapsAround() {
        var ctx = context("NETWORK", 13, Priority.P3);
        strategy.onAssigned(ctx, 2);                       // B took the last one
        assertEquals("A", strategy.rank(ctx, List.of(a, b)).get(0).name()); // C is full: wrap to A
    }
}
