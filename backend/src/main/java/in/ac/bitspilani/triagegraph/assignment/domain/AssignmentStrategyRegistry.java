package in.ac.bitspilani.triagegraph.assignment.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import in.ac.bitspilani.triagegraph.shared.exception.StrategyNotFoundException;

/**
 * Registry pattern: every {@link AssignmentStrategy} bean, looked up by key. The active key lives in the
 * settings table, so switching strategy at runtime needs no restart and no code change (Open/Closed).
 */
public final class AssignmentStrategyRegistry {

    private final Map<String, AssignmentStrategy> byKey;

    public AssignmentStrategyRegistry(List<AssignmentStrategy> strategies) {
        Map<String, AssignmentStrategy> map = new LinkedHashMap<>();
        for (AssignmentStrategy s : strategies) {
            String key = normalise(s.key());
            if (map.putIfAbsent(key, s) != null) {
                throw new IllegalStateException("Two assignment strategies use the key " + key);
            }
        }
        if (map.isEmpty()) {
            throw new IllegalStateException("At least one assignment strategy is required");
        }
        this.byKey = Collections.unmodifiableMap(map);
    }

    /** @throws StrategyNotFoundException unknown key */
    public AssignmentStrategy get(String key) {
        AssignmentStrategy s = key == null ? null : byKey.get(normalise(key));
        if (s == null) {
            throw new StrategyNotFoundException("Unknown assignment strategy '" + key + "'. Known: " + byKey.keySet());
        }
        return s;
    }

    public boolean contains(String key) {
        return key != null && byKey.containsKey(normalise(key));
    }

    public Collection<AssignmentStrategy> all() {
        return byKey.values();
    }

    public String firstKey() {
        return byKey.keySet().iterator().next();
    }

    private static String normalise(String key) {
        return key.trim().toUpperCase(Locale.ROOT);
    }
}
