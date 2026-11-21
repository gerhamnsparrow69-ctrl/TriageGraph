package in.ac.bitspilani.triagegraph.topology.rca.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;

class IncidentOpenLookupAdapterTest {

    @Test
    void returnsDeduplicatedServiceIdsOnlyForConfiguredOpenStatuses() {
        ObjectProvider<IncidentQueryPort> provider = mockProvider();
        IncidentQueryPort query = mock(IncidentQueryPort.class);
        when(provider.getIfAvailable()).thenReturn(query);
        when(query.openSince(java.time.Instant.EPOCH)).thenReturn(List.of(
                incident(1, 10, IncidentStatus.NEW),
                incident(2, 10, IncidentStatus.CLASSIFIED),
                incident(3, 11, IncidentStatus.ASSIGNED),
                incident(4, 12, IncidentStatus.ACKNOWLEDGED),
                incident(5, 13, IncidentStatus.IN_PROGRESS),
                incident(6, 14, IncidentStatus.PENDING_INFO),
                incident(7, 15, IncidentStatus.RESOLVED),
                incident(8, 16, IncidentStatus.VERIFIED),
                incident(9, 17, IncidentStatus.CLOSED),
                incident(10, 18, IncidentStatus.MERGED)));
        IncidentOpenLookupAdapter adapter = new IncidentOpenLookupAdapter(provider);

        assertThat(adapter.openIncidentServiceIds()).containsExactly(10L, 11L, 12L, 13L, 14L);
        verify(query).openSince(java.time.Instant.EPOCH);
    }

    @Test
    void missingProviderReturnsEmptyAndAvailableProviderFailurePropagates() {
        ObjectProvider<IncidentQueryPort> absent = mockProvider();
        when(absent.getIfAvailable()).thenReturn(null);
        IncidentOpenLookupAdapter unavailable = new IncidentOpenLookupAdapter(absent);
        Logger logger = (Logger) LoggerFactory.getLogger(IncidentOpenLookupAdapter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThat(unavailable.openIncidentServiceIds()).isEmpty();
            assertThat(unavailable.openIncidentServiceIds()).isEmpty();
            assertThat(appender.list)
                    .filteredOn(event -> event.getLevel() == Level.WARN)
                    .singleElement()
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .isEqualTo("IncidentQueryPort is unavailable; RCA open-incident evidence bonus is temporarily unavailable");
        } finally {
            logger.detachAppender(appender);
        }

        ObjectProvider<IncidentQueryPort> present = mockProvider();
        IncidentQueryPort query = mock(IncidentQueryPort.class);
        when(present.getIfAvailable()).thenReturn(query);
        RuntimeException failure = new RuntimeException("incident query failed");
        when(query.openSince(java.time.Instant.EPOCH)).thenThrow(failure);
        assertThatThrownBy(() -> new IncidentOpenLookupAdapter(present).openIncidentServiceIds())
                .isSameAs(failure);
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<IncidentQueryPort> mockProvider() {
        return mock(ObjectProvider.class);
    }

    private static IncidentSnapshot incident(long id, long serviceId, IncidentStatus status) {
        return new IncidentSnapshot(id, "INC-" + id, "title", "description", status,
                null, serviceId, null, 1, Set.of(), null, null,
                null, null, null);
    }
}
