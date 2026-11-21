package in.ac.bitspilani.triagegraph.topology.rca.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.topology.rca.domain.Evidence;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceAggregator;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceContext;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceProvider;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceType;

class EvidenceCollectorTest {

    private static final Instant NOW = Instant.parse("2025-01-01T12:00:00Z");

    @Test
    void combinesProvidersKeepsHighestBonusPerTypeAndCapsTheTotal() {
        Clock clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW);
        EvidenceProvider firstRecentChange = provider(new Evidence(
                EvidenceType.RECENT_CHANGE, 0.10, "Change logged 2 min ago: first"));
        EvidenceProvider secondRecentChange = provider(new Evidence(
                EvidenceType.RECENT_CHANGE, 0.12, "Change logged 1 min ago: second"));
        EvidenceProvider openIncident = provider(new Evidence(
                EvidenceType.OPEN_INCIDENT, 0.08, "Open incident on this service"));
        EvidenceCollector collector = new EvidenceCollector(
                List.of(firstRecentChange, secondRecentChange, openIncident),
                new EvidenceAggregator(0.15),
                clock);

        var result = collector.collect(List.of(1L));

        assertThat(result.get(1L).bonus()).isEqualTo(0.15);
        assertThat(result.get(1L).evidence()).containsExactly(
                new Evidence(EvidenceType.RECENT_CHANGE, 0.12, "Change logged 1 min ago: second"),
                new Evidence(EvidenceType.OPEN_INCIDENT, 0.08, "Open incident on this service"));
        verify(firstRecentChange).collect(eq(Set.of(1L)), eq(new EvidenceContext(NOW)));
        verify(secondRecentChange).collect(eq(Set.of(1L)), eq(new EvidenceContext(NOW)));
        verify(openIncident).collect(eq(Set.of(1L)), eq(new EvidenceContext(NOW)));
    }

    @Test
    void emptyCandidatesDoNotReadClockOrCallProviders() {
        Clock clock = mock(Clock.class);
        EvidenceProvider provider = mock(EvidenceProvider.class);
        EvidenceCollector collector = new EvidenceCollector(
                List.of(provider), new EvidenceAggregator(0.15), clock);

        assertThat(collector.collect(List.of())).isEmpty();
        verifyNoInteractions(clock, provider);
    }

    private static EvidenceProvider provider(Evidence evidence) {
        EvidenceProvider provider = mock(EvidenceProvider.class);
        when(provider.collect(any(), any())).thenReturn(Map.of(1L, List.of(evidence)));
        return provider;
    }
}
