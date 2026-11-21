package in.ac.bitspilani.triagegraph.shared.contracts.ports;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;

/** Provided by Member 1 (incident). */
public interface IncidentCommandPort {

    void markSlaWarning(long incidentId, SlaKind kind);

    void markSlaBreached(long incidentId, SlaKind kind);

    void escalate(long incidentId, int level, long targetUserId, String reason);

    void reassign(long incidentId, long technicianId, String reason, long actorId);

    void joinAsDuplicate(long incidentId, long userId);

    void merge(long childId, long parentId, long actorId);
}
