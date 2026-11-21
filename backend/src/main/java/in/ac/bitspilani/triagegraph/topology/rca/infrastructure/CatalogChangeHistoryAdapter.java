package in.ac.bitspilani.triagegraph.topology.rca.infrastructure;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.catalog.infrastructure.ChangeRecordRepository;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ChangeHistory;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ChangeInfo;

@Component
public class CatalogChangeHistoryAdapter implements ChangeHistory {

    private final ChangeRecordRepository changes;

    public CatalogChangeHistoryAdapter(ChangeRecordRepository changes) {
        this.changes = changes;
    }

    @Override
    public List<ChangeInfo> recentChanges(Set<Long> serviceIds, Instant fromInclusive, Instant toInclusive) {
        if (serviceIds.isEmpty()) {
            return List.of();
        }
        return changes.findRecentChanges(serviceIds, fromInclusive, toInclusive).stream()
                .map(change -> new ChangeInfo(
                        change.getService().getId(),
                        change.getDescription(),
                        change.getLoggedAt()))
                .toList();
    }
}
