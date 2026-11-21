package in.ac.bitspilani.triagegraph.priority.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.priority.domain.PriorityCalculator;
import in.ac.bitspilani.triagegraph.priority.domain.WeightedScorePriorityCalculator;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityInput;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.exception.StrategyNotFoundException;

class PriorityCalculatorRegistryTest {

    /** Stand-in for a second strategy, to show the registry is open for extension. */
    private static PriorityCalculator fixed(String key) {
        return new PriorityCalculator() {
            @Override public String key() { return key; }
            @Override public String description() { return "test"; }
            @Override public PriorityAssessment calculate(PriorityInput in) {
                return new PriorityAssessment(Priority.P3, 0, key, "fixed");
            }
        };
    }

    @Test
    void defaultsToWeightedScore() {
        PriorityCalculatorRegistry registry = new PriorityCalculatorRegistry(
                List.of(fixed("OTHER"), new WeightedScorePriorityCalculator()));
        assertThat(registry.activeKey()).isEqualTo("WEIGHTED_SCORE");
        assertThat(registry.active()).isInstanceOf(WeightedScorePriorityCalculator.class);
        assertThat(registry.all()).extracting(PriorityCalculator::key).containsExactly("OTHER", "WEIGHTED_SCORE");
    }

    @Test
    void unknownKeyThrowsStrategyNotFound() {
        PriorityCalculatorRegistry registry = new PriorityCalculatorRegistry(List.of(new WeightedScorePriorityCalculator()));
        assertThatThrownBy(() -> registry.get("NOPE"))
                .isInstanceOf(StrategyNotFoundException.class)
                .hasMessageContaining("NOPE");
    }

    @Test
    void duplicateKeysAreRejectedAtStartup() {
        assertThatThrownBy(() -> new PriorityCalculatorRegistry(
                List.of(new WeightedScorePriorityCalculator(), fixed("WEIGHTED_SCORE"))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingDefaultCalculatorFailsFast() {
        assertThatThrownBy(() -> new PriorityCalculatorRegistry(List.of(fixed("OTHER"))))
                .isInstanceOf(StrategyNotFoundException.class);
    }
}
