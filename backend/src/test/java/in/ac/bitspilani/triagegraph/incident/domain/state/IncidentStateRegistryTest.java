package in.ac.bitspilani.triagegraph.incident.domain.state;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentStateRegistryTest {

    static final Map<IncidentStatus, Set<IncidentStatus>> NEXT = new EnumMap<>(Map.of(
            NEW, EnumSet.of(CLASSIFIED, MERGED),
            CLASSIFIED, EnumSet.of(ASSIGNED, MERGED),
            ASSIGNED, EnumSet.of(ACKNOWLEDGED, ASSIGNED),
            ACKNOWLEDGED, EnumSet.of(IN_PROGRESS, ASSIGNED),
            IN_PROGRESS, EnumSet.of(PENDING_INFO, RESOLVED),
            PENDING_INFO, EnumSet.of(IN_PROGRESS),
            RESOLVED, EnumSet.of(VERIFIED, IN_PROGRESS),
            VERIFIED, EnumSet.of(CLOSED),
            CLOSED, EnumSet.noneOf(IncidentStatus.class),
            MERGED, EnumSet.noneOf(IncidentStatus.class)));

    @ParameterizedTest
    @EnumSource(IncidentStatus.class)
    void everyStatusHasItsOwnStateWithTheDocumentedNextStatuses(IncidentStatus s) {
        IncidentState state = IncidentStateRegistry.of(s);

        assertThat(state.status()).isEqualTo(s);
        assertThat(state.allowedNext()).isEqualTo(NEXT.get(s));
        assertThat(state.allowedActions()).isEqualTo(IncidentStateTransitionTest.LEGAL.get(s));
        assertThat(state.isTerminal()).isEqualTo(s == CLOSED || s == MERGED);
    }

    @Test
    void tenDistinctSingletonStates() {
        assertThat(Arrays.stream(IncidentStatus.values()).map(IncidentStateRegistry::of).distinct()).hasSize(10);
        assertThat(IncidentStateRegistry.of(NEW)).isSameAs(IncidentStateRegistry.of(NEW));
    }

    @Test
    void allowedSetsCannotBeModified() {
        Set<IncidentStatus> next = IncidentStateRegistry.of(NEW).allowedNext();
        Set<IncidentAction> actions = IncidentStateRegistry.of(NEW).allowedActions();
        assertThatThrownBy(() -> next.add(CLOSED)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> actions.add(IncidentAction.CLOSE)).isInstanceOf(UnsupportedOperationException.class);
    }
}
