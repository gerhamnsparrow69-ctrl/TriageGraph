package in.ac.bitspilani.triagegraph.topology.rca.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.topology.rca.domain.ChangeHistory;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ChangeInfo;
import in.ac.bitspilani.triagegraph.topology.rca.domain.Evidence;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceContext;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceType;
import in.ac.bitspilani.triagegraph.topology.rca.domain.RcaConfig;

class RecentChangeEvidenceTest {

    private static final Instant NOW = Instant.parse("2025-01-01T12:00:00Z");
    private static final Instant FROM = NOW.minusSeconds(2 * 60 * 60);

    @Test
    void includesExactTwoHourBoundaryAndUsesNewestValidRecordOnly() {
        ChangeHistory history = mock(ChangeHistory.class);
        when(history.recentChanges(eq(Set.of(1L)), eq(FROM), eq(NOW))).thenReturn(List.of(
                new ChangeInfo(1, "boundary", FROM),
                new ChangeInfo(1, "outside", FROM.minusSeconds(1)),
                new ChangeInfo(1, "future", NOW.plusSeconds(1)),
                new ChangeInfo(1, "null-time", null),
                new ChangeInfo(1, "latest", NOW.minusSeconds(60))));
        RecentChangeEvidence evidence = new RecentChangeEvidence(history, RcaConfig.defaults());

        Map<Long, List<Evidence>> result = evidence.collect(Set.of(1L), new EvidenceContext(NOW));

        assertThat(result).containsOnlyKeys(1L);
        assertThat(result.get(1L)).containsExactly(new Evidence(
                EvidenceType.RECENT_CHANGE, 0.10, "Change logged 1 min ago: latest"));
        verify(history).recentChanges(Set.of(1L), FROM, NOW);
    }

    @Test
    void missingRecordsProduceNoEvidenceAndRepositoryFailuresPropagate() {
        ChangeHistory history = mock(ChangeHistory.class);
        when(history.recentChanges(Set.of(1L), FROM, NOW)).thenReturn(List.of());
        RecentChangeEvidence evidence = new RecentChangeEvidence(history, RcaConfig.defaults());
        assertThat(evidence.collect(Set.of(1L), new EvidenceContext(NOW))).isEmpty();

        RuntimeException failure = new RuntimeException("database unavailable");
        when(history.recentChanges(Set.of(2L), FROM, NOW)).thenThrow(failure);
        assertThatThrownBy(() -> evidence.collect(Set.of(2L), new EvidenceContext(NOW)))
                .isSameAs(failure);
    }

    @Test
    void emptyCandidateSetDoesNotQueryChangeHistory() {
        ChangeHistory history = mock(ChangeHistory.class);
        RecentChangeEvidence evidence = new RecentChangeEvidence(history, RcaConfig.defaults());

        assertThat(evidence.collect(Set.of(), new EvidenceContext(NOW))).isEmpty();
        org.mockito.Mockito.verifyNoInteractions(history);
    }
}
