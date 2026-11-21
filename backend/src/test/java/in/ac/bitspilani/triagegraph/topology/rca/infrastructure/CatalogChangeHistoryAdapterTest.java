package in.ac.bitspilani.triagegraph.topology.rca.infrastructure;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.catalog.infrastructure.ChangeRecordRepository;

class CatalogChangeHistoryAdapterTest {

    @Test
    void repositoryFailurePropagatesUnchanged() {
        ChangeRecordRepository changes = mock(ChangeRecordRepository.class);
        CatalogChangeHistoryAdapter adapter = new CatalogChangeHistoryAdapter(changes);
        Set<Long> serviceIds = Set.of(17L, 23L);
        Instant from = Instant.parse("2025-01-01T10:00:00Z");
        Instant to = Instant.parse("2025-01-01T12:00:00Z");
        RuntimeException failure = new IllegalStateException("change history unavailable");
        when(changes.findRecentChanges(serviceIds, from, to)).thenThrow(failure);

        assertThatThrownBy(() -> adapter.recentChanges(serviceIds, from, to))
                .isSameAs(failure);
    }
}
