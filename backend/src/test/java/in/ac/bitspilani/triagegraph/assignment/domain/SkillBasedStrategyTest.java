package in.ac.bitspilani.triagegraph.assignment.domain;

import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.ananya;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.arjun;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.context;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.ravi;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.sneha;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.tech;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

class SkillBasedStrategyTest {

    private final EligibilityFilter filter = new EligibilityFilter();
    private final SkillBasedStrategy strategy = new SkillBasedStrategy();

    /** TC03: three technicians, only one with the skill → that one is chosen. */
    @Test
    void chooseTheOnlySkilledTechnician() {
        var eligible = filter.eligible("NETWORK", List.of(sneha(0), ravi(3), arjun(0)));
        List<TechnicianRecommendation> ranked = strategy.rank(context("NETWORK", 13, Priority.P3), eligible);
        assertEquals("Ravi Kumar", ranked.get(0).name());
        assertEquals(SkillBasedStrategy.KEY, ranked.get(0).strategyKey());
    }

    /** TC03: equally specialised technicians → the one with the lower load wins the tie. */
    @Test
    void tieBetweenEquallySkilledGoesToLowerLoad() {
        var busy = tech(10, "Busy", Set.of("APPLICATION", "DATABASE"), 4, 6, 3);
        var free = tech(11, "Free", Set.of("APPLICATION", "DATABASE"), 1, 6, 3);
        var ranked = strategy.rank(context("APPLICATION", 1, Priority.P3), List.of(busy, free));
        assertEquals("Free", ranked.get(0).name());
    }

    @Test
    void specialistBeatsGeneralistAtEqualLoad() {
        var specialist = tech(20, "Specialist", Set.of("APPLICATION"), 2, 6, 3);
        var generalist = tech(21, "Generalist", Set.of("APPLICATION", "DATABASE", "SOFTWARE"), 2, 6, 3);
        var ranked = strategy.rank(context("APPLICATION", 1, Priority.P3), List.of(generalist, specialist));
        assertEquals("Specialist", ranked.get(0).name());
        assertTrue(ranked.get(0).reasons().get(0).contains("specialist"));
    }

    @Test
    void scoresStayBetweenZeroAndOne() {
        for (var r : strategy.rank(context("APPLICATION", 1, Priority.P2), List.of(sneha(0), ananya(3)))) {
            assertTrue(r.score() >= 0 && r.score() <= 1, "score " + r.score());
        }
    }
}
