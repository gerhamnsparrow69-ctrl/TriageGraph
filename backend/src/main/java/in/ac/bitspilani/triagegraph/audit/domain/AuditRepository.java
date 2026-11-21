package in.ac.bitspilani.triagegraph.audit.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;

import java.util.List;
import java.util.Optional;

/**
 * Append-only storage for the audit chain. Deliberately has no update or delete (Interface Segregation):
 * nothing in the application can rewrite history.
 */
public interface AuditRepository {

    AuditEntry append(AuditEntry entry);

    /** The newest entry: its hash is the next entry's prevHash. */
    Optional<AuditEntry> latest();

    List<AuditEntry> forIncident(long incidentId);

    /** The status the incident was last recorded as reaching, if any. */
    Optional<IncidentStatus> lastKnownState(long incidentId);

    /** Up to {@code limit} entries with id greater than {@code afterId}, oldest first (for verification). */
    List<AuditEntry> batchAfter(long afterId, int limit);

    /** Newest first, for the admin audit page. */
    List<AuditEntry> page(int page, int size);

    long count();
}
