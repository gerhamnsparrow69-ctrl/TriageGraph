package in.ac.bitspilani.triagegraph.priority.application;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.priority.domain.PriorityCalculator;
import in.ac.bitspilani.triagegraph.priority.domain.WeightedScorePriorityCalculator;
import in.ac.bitspilani.triagegraph.shared.exception.StrategyNotFoundException;

/**
 * Registry pattern: Spring hands over every {@link PriorityCalculator} bean, and the registry
 * looks one up by key. Adding a calculator is a new class, with no edits here (Open/Closed).
 * <p>
 * MVP: the active key is held in memory and starts as {@code WEIGHTED_SCORE}. The runtime switch
 * (PUT endpoint + system_settings row) comes later.
 */
@Component
public class PriorityCalculatorRegistry {

    public static final String DEFAULT_KEY = WeightedScorePriorityCalculator.KEY;

    private final Map<String, PriorityCalculator> byKey;
    private final AtomicReference<String> activeKey;

    public PriorityCalculatorRegistry(List<PriorityCalculator> calculators) {
        Map<String, PriorityCalculator> map = new TreeMap<>();
        for (PriorityCalculator c : calculators) {
            PriorityCalculator previous = map.putIfAbsent(c.key(), c);
            if (previous != null) {
                throw new IllegalStateException("Two priority calculators share the key " + c.key());
            }
        }
        if (!map.containsKey(DEFAULT_KEY)) {
            throw new StrategyNotFoundException("Default priority calculator " + DEFAULT_KEY + " is not registered.");
        }
        this.byKey = Collections.unmodifiableMap(map);
        this.activeKey = new AtomicReference<>(DEFAULT_KEY);
    }

    public PriorityCalculator active() {
        return get(activeKey.get());
    }

    public String activeKey() {
        return activeKey.get();
    }

    public PriorityCalculator get(String key) {
        PriorityCalculator calculator = byKey.get(key);
        if (calculator == null) {
            throw new StrategyNotFoundException("Unknown priority calculator: " + key
                    + ". Available: " + byKey.keySet());
        }
        return calculator;
    }

    /** All registered calculators, sorted by key. */
    public List<PriorityCalculator> all() {
        return List.copyOf(byKey.values());
    }
}
