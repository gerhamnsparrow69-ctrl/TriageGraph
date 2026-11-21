package in.ac.bitspilani.triagegraph.topology.rca.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.topology.rca.application.CoverageSpecificityRanker;
import in.ac.bitspilani.triagegraph.topology.rca.application.EvidenceCollector;
import in.ac.bitspilani.triagegraph.topology.rca.application.OwnIncidentEvidence;
import in.ac.bitspilani.triagegraph.topology.rca.application.RcaConfiguration;
import in.ac.bitspilani.triagegraph.topology.rca.application.RecentChangeEvidence;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ChangeHistory;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ImpactTopology;

class RcaOptionalIncidentWiringTest {

    @Test
    void rankerStartsAndReturnsNoIncidentBonusWithoutIncidentQueryPort() {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(RcaWithoutIncidentProvider.class)) {
            assertThat(context.getBeansOfType(IncidentQueryPort.class)).isEmpty();

            var result = context.getBean(CoverageSpecificityRanker.class).rank(Set.of(1L));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).score()).isEqualTo(0.85);
            assertThat(result.get(0).evidence()).isEmpty();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            RcaConfiguration.class,
            CoverageSpecificityRanker.class,
            EvidenceCollector.class,
            RecentChangeEvidence.class,
            OwnIncidentEvidence.class,
            IncidentOpenLookupAdapter.class
    })
    static class RcaWithoutIncidentProvider {

        @Bean
        Clock rcaTestClock() {
            return Clock.fixed(Instant.parse("2025-01-01T12:00:00Z"), ZoneOffset.UTC);
        }

        @Bean
        ChangeHistory changeHistory() {
            return (serviceIds, fromInclusive, toInclusive) -> List.of();
        }

        @Bean
        CatalogQuery catalogQuery() {
            CatalogQuery catalog = mock(CatalogQuery.class);
            when(catalog.services()).thenReturn(List.of(
                    new ServiceRef(1L, "TEST", "Test service", null)));
            return catalog;
        }

        @Bean
        TopologyImpactAdapter topologyImpactAdapter() {
            TopologyImpactAdapter topology = mock(TopologyImpactAdapter.class);
            when(topology.snapshot()).thenReturn(new SingleServiceTopology());
            return topology;
        }
    }

    private static final class SingleServiceTopology implements ImpactTopology {

        @Override
        public boolean contains(long serviceId) {
            return serviceId == 1L;
        }

        @Override
        public Set<Long> serviceIds() {
            return Set.of(1L);
        }

        @Override
        public Set<Long> dependenciesOf(long serviceId) {
            return Set.of();
        }

        @Override
        public Set<Long> dependentsOf(long serviceId) {
            return Set.of();
        }

        @Override
        public long version() {
            return 1L;
        }
    }
}
