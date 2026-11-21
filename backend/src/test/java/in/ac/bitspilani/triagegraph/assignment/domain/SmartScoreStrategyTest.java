package in.ac.bitspilani.triagegraph.assignment.domain;

import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.DATABASE_CLUSTER;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.FEE_PAYMENT_APP;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.ananya;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.context;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.sneha;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.withFixes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

class SmartScoreStrategyTest {

    private final SmartScoreStrategy strategy = new SmartScoreStrategy(id -> id == FEE_PAYMENT_APP ? "Fee Payment App" : "Other", 5);

    /**
     * The Workflow 1 example: P1 on the Fee Payment App. Sneha (load 1/6, 3 fixes there, 4 yrs) beats the
     * more senior team lead Ananya (load 3/4, 1 fix, 8 yrs).
     * Sneha  = 0.35 + 0.15·(5/6) + 0.20·(3/5) + 0.30·0.4 = 0.715
     * Ananya = 0.35 + 0.15·(1/4) + 0.20·(1/5) + 0.30·0.8 = 0.6675
     */
    @Test
    void workflowOneExamplePicksSnehaWithReadableReasons() {
        var s = withFixes(sneha(1), Map.of(FEE_PAYMENT_APP, 3, DATABASE_CLUSTER, 2));
        var a = withFixes(ananya(3), Map.of(FEE_PAYMENT_APP, 1));
        var ranked = strategy.rank(context("APPLICATION", FEE_PAYMENT_APP, Priority.P1), List.of(a, s));
        assertEquals("Sneha Reddy", ranked.get(0).name());
        assertEquals(0.715, ranked.get(0).score(), 1e-9);
        assertEquals(0.668, ranked.get(1).score(), 1e-9); // rounded to 3 decimals
        assertEquals(List.of("APPLICATION skill", "load 1/6", "3 past fixes on Fee Payment App", "4 yrs experience",
                "P1: seniority weighted higher"), ranked.get(0).reasons());
    }

    @Test
    void lowPriorityUsesNormalWeightsSoFreeCapacityMattersMore() {
        var s = sneha(1);
        var a = ananya(3);
        var ranked = strategy.rank(context("APPLICATION", 99, Priority.P4), List.of(a, s));
        // Sneha 0.40 + 0.25·0.833 + 0 + 0.15·0.4 = 0.668 ; Ananya 0.40 + 0.25·0.25 + 0 + 0.15·0.8 = 0.583
        assertEquals("Sneha Reddy", ranked.get(0).name());
        assertEquals(0.668, ranked.get(0).score(), 1e-9);
    }

    @Test
    void experienceIsCappedSoTenFixesCountTheSameAsFive() {
        var five = withFixes(sneha(1), Map.of(FEE_PAYMENT_APP, 5));
        var ten = withFixes(sneha(1), Map.of(FEE_PAYMENT_APP, 10));
        var ctx = context("APPLICATION", FEE_PAYMENT_APP, Priority.P3);
        assertEquals(strategy.rank(ctx, List.of(five)).get(0).score(), strategy.rank(ctx, List.of(ten)).get(0).score(), 1e-12);
    }

    @Test
    void weightsMustAddUpToOne() {
        assertThrows(IllegalArgumentException.class, () -> new SmartScoreStrategy.Weights(0.5, 0.5, 0.5, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new SmartScoreStrategy(id -> "x", 0));
    }

    @Test
    void noFixesMeansNoFixReason() {
        var ranked = strategy.rank(context("APPLICATION", FEE_PAYMENT_APP, Priority.P3), List.of(sneha(0)));
        assertTrue(ranked.get(0).reasons().stream().noneMatch(r -> r.contains("past fix")));
    }
}
