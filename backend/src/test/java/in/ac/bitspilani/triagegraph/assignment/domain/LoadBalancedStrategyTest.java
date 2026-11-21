package in.ac.bitspilani.triagegraph.assignment.domain;

import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.context;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.tech;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

class LoadBalancedStrategyTest {

    private final LoadBalancedStrategy strategy = new LoadBalancedStrategy();

    @Test
    void lowestLoadRatioRanksFirstEvenWithFewerFreeSlots() {
        var a = tech(1, "A", Set.of("APPLICATION"), 3, 6, 5);   // 50 %
        var b = tech(2, "B", Set.of("APPLICATION"), 1, 4, 5);   // 25 %
        var c = tech(3, "C", Set.of("APPLICATION"), 6, 8, 5);   // 75 %
        var ranked = strategy.rank(context("APPLICATION", 1, Priority.P3), List.of(a, b, c));
        assertEquals(List.of("B", "A", "C"), ranked.stream().map(r -> r.name()).toList());
        assertEquals(0.75, ranked.get(0).score(), 1e-9);
    }

    @Test
    void equalRatiosAreOrderedByIdSoResultsAreDeterministic() {
        var x = tech(9, "X", Set.of("APPLICATION"), 2, 4, 5);
        var y = tech(7, "Y", Set.of("APPLICATION"), 3, 6, 5);
        var ranked = strategy.rank(context("APPLICATION", 1, Priority.P3), List.of(x, y));
        assertEquals("Y", ranked.get(0).name());
    }
}
