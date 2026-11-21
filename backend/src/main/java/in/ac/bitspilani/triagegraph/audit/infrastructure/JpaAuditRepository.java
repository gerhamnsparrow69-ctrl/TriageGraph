package in.ac.bitspilani.triagegraph.audit.infrastructure;

import in.ac.bitspilani.triagegraph.audit.domain.AuditEntry;
import in.ac.bitspilani.triagegraph.audit.domain.AuditRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
class JpaAuditRepository implements AuditRepository {

    private final SpringDataAuditRepository jpa;

    JpaAuditRepository(SpringDataAuditRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public AuditEntry append(AuditEntry entry) {
        if (entry.getId() != null) {
            throw new IllegalArgumentException("Audit entries are append-only");
        }
        return jpa.save(entry);
    }

    @Override
    public Optional<AuditEntry> latest() {
        return jpa.findTopByOrderByIdDesc();
    }

    @Override
    public List<AuditEntry> forIncident(long incidentId) {
        return jpa.findByIncidentIdOrderByIdAsc(incidentId);
    }

    @Override
    public Optional<IncidentStatus> lastKnownState(long incidentId) {
        return jpa.findTopByIncidentIdAndToStateIsNotNullOrderByIdDesc(incidentId).map(AuditEntry::getToState);
    }

    @Override
    public List<AuditEntry> batchAfter(long afterId, int limit) {
        return jpa.findByIdGreaterThanOrderByIdAsc(afterId, PageRequest.of(0, limit));
    }

    @Override
    public List<AuditEntry> page(int page, int size) {
        return jpa.findAllByOrderByIdDesc(PageRequest.of(page, size));
    }

    @Override
    public long count() {
        return jpa.count();
    }
}
