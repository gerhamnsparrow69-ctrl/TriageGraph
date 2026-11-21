package in.ac.bitspilani.triagegraph.assignment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.exception.StrategyNotFoundException;

class AssignmentStrategyRegistryTest {

    private final AssignmentStrategyRegistry registry = new AssignmentStrategyRegistry(
            List.of(new LoadBalancedStrategy(), new RoundRobinStrategy()));

    @Test
    void looksUpByKeyIgnoringCase() {
        assertEquals(RoundRobinStrategy.KEY, registry.get("round_robin").key());
        assertTrue(registry.contains("LOAD_BALANCED"));
        assertFalse(registry.contains("SMART"));
        assertEquals(LoadBalancedStrategy.KEY, registry.firstKey());
    }

    @Test
    void unknownKeyThrowsStrategyNotFound() {
        var e = assertThrows(StrategyNotFoundException.class, () -> registry.get("FASTEST"));
        assertTrue(e.getMessage().contains("FASTEST"));
        assertThrows(StrategyNotFoundException.class, () -> registry.get(null));
    }

    @Test
    void twoStrategiesWithTheSameKeyAreRejected() {
        assertThrows(IllegalStateException.class,
                () -> new AssignmentStrategyRegistry(List.of(new LoadBalancedStrategy(), new LoadBalancedStrategy())));
        assertThrows(IllegalStateException.class, () -> new AssignmentStrategyRegistry(List.of()));
    }
}
