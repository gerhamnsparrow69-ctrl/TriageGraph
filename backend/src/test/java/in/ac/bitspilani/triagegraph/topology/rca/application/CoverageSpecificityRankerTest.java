package in.ac.bitspilani.triagegraph.topology.rca.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.RootCauseCandidate;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;
import in.ac.bitspilani.triagegraph.topology.rca.domain.AggregatedEvidence;
import in.ac.bitspilani.triagegraph.topology.rca.domain.CandidateGenerator;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ImpactTopology;
import in.ac.bitspilani.triagegraph.topology.rca.domain.RcaConfig;
import in.ac.bitspilani.triagegraph.topology.rca.domain.RcaScorer;
import in.ac.bitspilani.triagegraph.topology.rca.infrastructure.TopologyImpactAdapter;

class CoverageSpecificityRankerTest {

    @Test
    void emptyInputReturnsEmptyWithoutTouchingTopologyCatalogOrEvidenceProviders() {
        TopologyImpactAdapter topology = mock(TopologyImpactAdapter.class);
        CatalogQuery catalog = mock(CatalogQuery.class);
        EvidenceCollector evidence = mock(EvidenceCollector.class);
        CoverageSpecificityRanker ranker = ranker(topology, catalog, evidence);

        assertThat(ranker.rank(Set.of())).isEmpty();
        verifyNoInteractions(topology, catalog, evidence);
    }

    @Test
    void nullSetAndNullElementUseExistingInvalidInputException() {
        CoverageSpecificityRanker ranker = ranker(
                mock(TopologyImpactAdapter.class), mock(CatalogQuery.class), mock(EvidenceCollector.class));
        Set<Long> withNull = new HashSet<>();
        withNull.add(null);

        assertThatThrownBy(() -> ranker.rank(null)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> ranker.rank(withNull)).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void unknownServiceIsRejectedBeforeCatalogueOrEvidenceLookup() {
        TopologyImpactAdapter topology = mock(TopologyImpactAdapter.class);
        CatalogQuery catalog = mock(CatalogQuery.class);
        EvidenceCollector evidence = mock(EvidenceCollector.class);
        when(topology.snapshot()).thenReturn(topology(Set.of(1L), Map.of()));
        CoverageSpecificityRanker ranker = ranker(topology, catalog, evidence);

        assertThatThrownBy(() -> ranker.rank(Set.of(1L, 999L)))
                .isInstanceOf(ServiceNotFoundException.class);
        verify(topology).snapshot();
        verifyNoInteractions(catalog, evidence);
    }

    @Test
    void acquiresOneSnapshotAndMapsSharedCandidateDto() {
        TopologyImpactAdapter topologyAdapter = mock(TopologyImpactAdapter.class);
        CatalogQuery catalog = mock(CatalogQuery.class);
        EvidenceCollector evidence = mock(EvidenceCollector.class);
        when(topologyAdapter.snapshot()).thenReturn(topology(Set.of(1L), Map.of()));
        when(catalog.services()).thenReturn(List.of(new ServiceRef(1L, "LMS", "Learning Management", null)));
        when(evidence.collect(anyCollection())).thenReturn(Map.of(1L, AggregatedEvidence.none()));
        CoverageSpecificityRanker ranker = ranker(topologyAdapter, catalog, evidence);

        List<RootCauseCandidate> result = ranker.rank(Set.of(1L));

        assertThat(result).containsExactly(new RootCauseCandidate(
                1L, "Learning Management", 0.85, 1.0, 1.0, List.of()));
        verify(topologyAdapter).snapshot();
        verify(catalog).services();
        verify(evidence).collect(List.of(1L));
    }

    @Test
    void missingDisplayNameUsesFallbackWithoutDroppingCandidate() {
        TopologyImpactAdapter topologyAdapter = mock(TopologyImpactAdapter.class);
        CatalogQuery catalog = mock(CatalogQuery.class);
        EvidenceCollector evidence = mock(EvidenceCollector.class);
        when(topologyAdapter.snapshot()).thenReturn(topology(Set.of(47L), Map.of()));
        when(catalog.services()).thenReturn(List.of(new ServiceRef(47L, "X", null, null)));
        when(evidence.collect(anyCollection())).thenReturn(Map.of());
        CoverageSpecificityRanker ranker = ranker(topologyAdapter, catalog, evidence);

        assertThat(ranker.rank(Set.of(47L)).get(0).serviceName()).isEqualTo("Service 47");
        verify(catalog, never()).service(47L);
    }

    private static CoverageSpecificityRanker ranker(
            TopologyImpactAdapter topology,
            CatalogQuery catalog,
            EvidenceCollector evidence) {
        return new CoverageSpecificityRanker(
                new CandidateGenerator(), topology, catalog, evidence, new RcaScorer(RcaConfig.defaults()));
    }

    private static ImpactTopology topology(Set<Long> ids, Map<Long, Set<Long>> dependencies) {
        Map<Long, Set<Long>> dependents = new java.util.HashMap<>();
        dependencies.forEach((serviceId, dependencyIds) -> dependencyIds.forEach(dependencyId ->
                dependents.computeIfAbsent(dependencyId, ignored -> new java.util.HashSet<>()).add(serviceId)));
        return new ImpactTopology() {
            @Override
            public boolean contains(long serviceId) {
                return ids.contains(serviceId);
            }

            @Override
            public Set<Long> serviceIds() {
                return Set.copyOf(ids);
            }

            @Override
            public Set<Long> dependenciesOf(long serviceId) {
                Set<Long> found = dependencies.getOrDefault(serviceId, Set.of());
                return Set.copyOf(found);
            }

            @Override
            public Set<Long> dependentsOf(long serviceId) {
                return Set.copyOf(dependents.getOrDefault(serviceId, Set.of()));
            }

            @Override
            public long version() {
                return 1;
            }
        };
    }
}
