package in.ac.bitspilani.triagegraph.audit.application;

import in.ac.bitspilani.triagegraph.audit.domain.AuditEntry;
import in.ac.bitspilani.triagegraph.audit.domain.AuditHasher;
import in.ac.bitspilani.triagegraph.audit.domain.AuditRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import in.ac.bitspilani.triagegraph.shared.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Reads the audit trail: an incident's timeline, the admin listing, and chain verification. */
@Service
@Transactional(readOnly = true)
public class AuditService {

    static final int VERIFY_BATCH = 500;
    static final int MAX_PAGE_SIZE = 200;
    private static final Set<Role> SEE_ALL = Set.of(Role.TEAM_LEAD, Role.ADMIN, Role.IT_MANAGER);

    private final AuditRepository repository;
    private final IncidentQueryPort incidents;
    private final UserDirectory users;

    public AuditService(AuditRepository repository, IncidentQueryPort incidents, UserDirectory users) {
        this.repository = repository;
        this.incidents = incidents;
        this.users = users;
    }

    /** The incident page's timeline, for anyone who may open the incident. */
    public List<AuditEntryView> timeline(long incidentId, long viewerId, Role viewerRole) {
        IncidentSnapshot incident = incidents.find(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException("No incident with id " + incidentId));
        boolean allowed = SEE_ALL.contains(viewerRole)
                || incident.reporterId() == viewerId
                || (incident.followerIds() != null && incident.followerIds().contains(viewerId))
                || Long.valueOf(viewerId).equals(incident.assigneeId());
        if (!allowed) {
            throw new UnauthorizedOperationException("You do not have access to " + incident.code() + ".");
        }
        return views(repository.forIncident(incidentId));
    }

    /** Newest first, for /admin/audit. */
    public List<AuditEntryView> recent(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidInputException("page must be ≥ 0 and size between 1 and " + MAX_PAGE_SIZE + ".");
        }
        return views(repository.page(page, size));
    }

    /** Adds actor display names, looking each user up once per response. */
    private List<AuditEntryView> views(List<AuditEntry> entries) {
        Map<Long, String> cache = new HashMap<>();
        Function<Long, String> names = id -> id == null ? null : cache.computeIfAbsent(id, this::nameOf);
        return entries.stream().map(e -> AuditEntryView.of(e, names)).toList();
    }

    private String nameOf(long userId) {
        try {
            UserRef ref = users.user(userId);
            return ref == null || ref.name() == null || ref.name().isBlank() ? null : ref.name();
        } catch (UserNotFoundException e) {
            return null;
        }
    }

    /**
     * Walks the whole chain oldest first, in batches. Each entry must point at the previous entry's hash,
     * and its own hash must match a fresh SHA-256 of its fields. Reports the first entry that fails.
     */
    public AuditVerification verify() {
        String expectedPrev = AuditHasher.GENESIS;
        long checked = 0;
        long afterId = 0;
        List<AuditEntry> batch;
        do {
            batch = repository.batchAfter(afterId, VERIFY_BATCH);
            for (AuditEntry e : batch) {
                if (!e.getPrevHash().equals(expectedPrev)) {
                    return AuditVerification.broken(checked, e.getId(),
                            "it does not link to the previous entry (an entry was removed or reordered)");
                }
                if (!e.computeHash().equals(e.getHash())) {
                    return AuditVerification.broken(checked, e.getId(), "its contents were changed after it was written");
                }
                expectedPrev = e.getHash();
                checked++;
                afterId = e.getId();
            }
        } while (batch.size() == VERIFY_BATCH);
        return AuditVerification.ok(checked);
    }
}
