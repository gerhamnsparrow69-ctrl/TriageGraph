package in.ac.bitspilani.triagegraph.audit.infrastructure;

import in.ac.bitspilani.triagegraph.audit.domain.AuditEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Extends the bare {@link Repository} marker rather than JpaRepository, so it inherits no delete or
 * update methods; only the reads and the single save below exist.
 */
interface SpringDataAuditRepository extends Repository<AuditEntry, Long> {

    AuditEntry save(AuditEntry entry);

    Optional<AuditEntry> findTopByOrderByIdDesc();

    List<AuditEntry> findByIncidentIdOrderByIdAsc(Long incidentId);

    Optional<AuditEntry> findTopByIncidentIdAndToStateIsNotNullOrderByIdDesc(Long incidentId);

    List<AuditEntry> findByIdGreaterThanOrderByIdAsc(Long afterId, Pageable page);

    List<AuditEntry> findAllByOrderByIdDesc(Pageable page);

    long count();
}
