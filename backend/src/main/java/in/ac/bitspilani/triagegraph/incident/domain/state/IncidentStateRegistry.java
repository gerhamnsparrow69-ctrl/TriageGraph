package in.ac.bitspilani.triagegraph.incident.domain.state;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Maps each status to its singleton state object. An {@link EnumMap} gives O(1), type-safe lookup
 * with no if/else chain; the static check below fails fast if a status ever lacks a state.
 */
public final class IncidentStateRegistry {

    private static final Map<IncidentStatus, IncidentState> STATES = new EnumMap<>(IncidentStatus.class);

    static {
        List.of(new NewState(), new ClassifiedState(), new AssignedState(), new AcknowledgedState(),
                        new InProgressState(), new PendingInfoState(), new ResolvedState(), new VerifiedState(),
                        new ClosedState(), new MergedState())
                .forEach(s -> STATES.put(s.status(), s));
        if (STATES.size() != IncidentStatus.values().length) {
            throw new IllegalStateException("Every IncidentStatus needs an IncidentState");
        }
    }

    private IncidentStateRegistry() {
    }

    public static IncidentState of(IncidentStatus status) {
        return STATES.get(status);
    }
}
