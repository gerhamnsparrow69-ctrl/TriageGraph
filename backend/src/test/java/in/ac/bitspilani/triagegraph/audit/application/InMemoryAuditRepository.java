package in.ac.bitspilani.triagegraph.audit.application;

import in.ac.bitspilani.triagegraph.audit.domain.AuditEntry;
import in.ac.bitspilani.triagegraph.audit.domain.AuditRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

class InMemoryAuditRepository implements AuditRepository {

    final List<AuditEntry> rows = new ArrayList<>();
    private long nextId = 1;

    @Override
    public AuditEntry append(AuditEntry entry) {
        ReflectionTestUtils.setField(entry, "id", nextId++);
        rows.add(entry);
        return entry;
    }

    @Override
    public Optional<AuditEntry> latest() {
        return rows.stream().max(Comparator.comparing(AuditEntry::getId));
    }

    @Override
    public List<AuditEntry> forIncident(long incidentId) {
        return rows.stream().filter(e -> Objects.equals(e.getIncidentId(), incidentId)).toList();
    }

    @Override
    public Optional<IncidentStatus> lastKnownState(long incidentId) {
        return forIncident(incidentId).stream().map(AuditEntry::getToState).filter(Objects::nonNull)
                .reduce((a, b) -> b);
    }

    @Override
    public List<AuditEntry> batchAfter(long afterId, int limit) {
        return rows.stream().filter(e -> e.getId() > afterId).limit(limit).toList();
    }

    @Override
    public List<AuditEntry> page(int page, int size) {
        return rows.reversed().stream().skip((long) page * size).limit(size).toList();
    }

    @Override
    public long count() {
        return rows.size();
    }
}
