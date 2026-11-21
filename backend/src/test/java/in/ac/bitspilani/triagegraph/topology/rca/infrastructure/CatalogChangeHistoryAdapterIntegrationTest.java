package in.ac.bitspilani.triagegraph.topology.rca.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.catalog.domain.ChangeRecord;
import in.ac.bitspilani.triagegraph.catalog.domain.ITService;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ChangeRecordRepository;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ITServiceRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.RootCauseCandidate;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.RootCauseRanker;

@SpringBootTest
@Import(CatalogChangeHistoryAdapterIntegrationTest.FixedClockConfiguration.class)
@ActiveProfiles({"test", "demo"})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:rcachangeintegration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@Transactional
class CatalogChangeHistoryAdapterIntegrationTest {

    private static final Instant NOW = Instant.parse("2025-01-01T12:00:00Z");

    @Autowired
    CatalogChangeHistoryAdapter history;

    @Autowired
    ChangeRecordRepository changes;

    @Autowired
    ITServiceRepository services;

    @Autowired
    RootCauseRanker ranker;

    @MockitoBean
    IncidentQueryPort incidents;

    @TestConfiguration
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    @Test
    void repositoryReturnsInclusiveRangeForRequestedServicesOnly() {
        ITService lms = services.findByCode("LMS").orElseThrow();
        ITService erp = services.findByCode("ERP Portal").orElseThrow();
        Instant now = Instant.parse("2025-01-01T12:00:00Z");
        Instant from = now.minusSeconds(2 * 60 * 60);
        changes.saveAndFlush(new ChangeRecord(lms, "at lower bound", from));
        changes.saveAndFlush(new ChangeRecord(lms, "in range", now.minusSeconds(30)));
        changes.saveAndFlush(new ChangeRecord(lms, "before range", from.minusSeconds(1)));
        changes.saveAndFlush(new ChangeRecord(lms, "after range", now.plusSeconds(1)));
        changes.saveAndFlush(new ChangeRecord(erp, "other service", now));

        var result = history.recentChanges(Set.of(lms.getId()), from, now);

        assertThat(result).extracting(change -> change.description())
                .containsExactly("in range", "at lower bound");
        assertThat(result).extracting(change -> change.serviceId()).containsOnly(lms.getId());
    }

    @Test
    void rankerIntegratesSeededGraphPersistedChangeAndOpenIncidents() {
        ITService lms = services.findByCode("LMS").orElseThrow();
        ITService erp = services.findByCode("ERP Portal").orElseThrow();
        ITService email = services.findByCode("Email & Collaboration").orElseThrow();
        ITService sso = services.findByCode("Campus SSO").orElseThrow();
        changes.saveAndFlush(new ChangeRecord(sso, "authentication rollout", NOW.minusSeconds(600)));
        when(incidents.openSince(Instant.EPOCH)).thenReturn(List.of(
                incident(1, lms.getId(), IncidentStatus.NEW),
                incident(2, erp.getId(), IncidentStatus.ASSIGNED),
                incident(3, email.getId(), IncidentStatus.IN_PROGRESS)));

        List<RootCauseCandidate> result = ranker.rank(Set.of(lms.getId(), erp.getId(), email.getId()));
        Map<String, RootCauseCandidate> byName = result.stream()
                .collect(java.util.stream.Collectors.toMap(RootCauseCandidate::serviceName, candidate -> candidate));

        assertThat(result).extracting(RootCauseCandidate::serviceName)
                .startsWith("Campus SSO", "LDAP Directory", "Core Campus Network",
                        "Data-Centre Power", "Database Cluster");
        assertThat(byName.get("Campus SSO").score()).isCloseTo(0.7833333333333333,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(byName.get("LDAP Directory").score()).isCloseTo(0.675,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(byName.get("LMS").score()).isCloseTo(0.5,
                org.assertj.core.data.Offset.offset(1e-12));
        assertThat(byName.get("Campus SSO").evidence())
                .containsExactly("Change logged 10 min ago: authentication rollout");
    }

    private static IncidentSnapshot incident(long id, long serviceId, IncidentStatus status) {
        return new IncidentSnapshot(id, "INC-" + id, "title", "description", status,
                null, serviceId, null, 1, Set.of(), null, null,
                null, null, null);
    }
}
