package in.ac.bitspilani.triagegraph.assignment.domain;

import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.FEE_PAYMENT_APP;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.ananya;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.context;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.sneha;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.tech;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.withFixes;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/**
 * Liskov substitution check: the same contract runs against every strategy, so the lifecycle can use any of
 * them interchangeably. A new strategy only needs to be added to {@link #all()}.
 */
class AssignmentStrategyContractTest {

    private static List<AssignmentStrategy> all() {
        return List.of(new SmartScoreStrategy(id -> "Fee Payment App", 5), new SkillBasedStrategy(),
                new LoadBalancedStrategy(), new RoundRobinStrategy());
    }

    private final List<TechnicianProfile> eligible = List.of(
            withFixes(sneha(1), Map.of(FEE_PAYMENT_APP, 3)), ananya(2),
            tech(12, "Kiran", Set.of("APPLICATION"), 0, 5, 1));

    @Test
    void everyStrategyReturnsEachEligibleTechnicianExactlyOnce() {
        for (AssignmentStrategy s : all()) {
            List<TechnicianRecommendation> ranked = s.rank(context("APPLICATION", FEE_PAYMENT_APP, Priority.P2), eligible);
            assertEquals(eligible.size(), ranked.size(), s.key());
            Set<Long> ids = new HashSet<>();
            ranked.forEach(r -> ids.add(r.technicianId()));
            assertEquals(Set.of(4L, 2L, 12L), ids, s.key());
        }
    }

    @Test
    void everyStrategySortsScoresInZeroToOneBestFirst() {
        for (AssignmentStrategy s : all()) {
            List<TechnicianRecommendation> ranked = s.rank(context("APPLICATION", FEE_PAYMENT_APP, Priority.P3), eligible);
            for (int i = 0; i < ranked.size(); i++) {
                double score = ranked.get(i).score();
                assertTrue(score >= 0.0 && score <= 1.0, s.key() + " score " + score);
                if (i > 0) {
                    assertTrue(ranked.get(i - 1).score() >= score, s.key() + " not sorted");
                }
            }
        }
    }

    @Test
    void everyRecommendationNamesItsStrategyAndGivesReasons() {
        for (AssignmentStrategy s : all()) {
            for (TechnicianRecommendation r : s.rank(context("APPLICATION", FEE_PAYMENT_APP, Priority.P1), eligible)) {
                assertEquals(s.key(), r.strategyKey());
                assertFalse(r.reasons().isEmpty(), s.key() + " gave no reasons");
            }
        }
    }

    @Test
    void rankingIsRepeatableBecauseItHasNoSideEffects() {
        for (AssignmentStrategy s : all()) {
            var ctx = context("APPLICATION", FEE_PAYMENT_APP, Priority.P2);
            assertEquals(s.rank(ctx, eligible), s.rank(ctx, eligible), s.key());
        }
    }

    @Test
    void keysAreUniqueUpperCaseWords() {
        Set<String> keys = new HashSet<>();
        for (AssignmentStrategy s : all()) {
            assertTrue(s.key().matches("[A-Z_]+"), s.key());
            assertTrue(keys.add(s.key()), "duplicate " + s.key());
            assertFalse(s.displayName().isBlank());
            assertFalse(s.description().isBlank());
        }
    }
}
