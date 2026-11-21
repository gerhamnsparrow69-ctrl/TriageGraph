package in.ac.bitspilani.triagegraph.shared.contracts.enums;

/** The 10 lifecycle states. Transitions are owned by the IncidentState classes (Member 1). */
public enum IncidentStatus {
    NEW, CLASSIFIED, ASSIGNED, ACKNOWLEDGED, IN_PROGRESS, PENDING_INFO,
    RESOLVED, VERIFIED, CLOSED, MERGED
}
