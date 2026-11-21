package in.ac.bitspilani.triagegraph.triage.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.SimilarIncident;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;

/**
 * Immutable read model of one knowledge-base entry, kept next to its vector in the {@link VectorIndex}.
 * Archive entries have no live incident; their public id is the negative document id, so the UI can tell
 * them apart from real incidents (positive ids).
 */
public record KnowledgeView(long docId, Long incidentId, String code, String title, IncidentStatus status,
        String categoryCode, Long serviceId, String serviceName, Instant createdAt, Long reporterId,
        String resolutionSummary, String rootCause, Long resolverId, KnowledgeSource source) {

    private static final Set<IncidentStatus> DONE = EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED,
            IncidentStatus.CLOSED, IncidentStatus.MERGED);
    private static final Set<IncidentStatus> RESOLVED = EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED,
            IncidentStatus.CLOSED);

    /** Still being worked on, so a new report could be a duplicate of it. */
    public boolean isOpen() {
        return status != null && !DONE.contains(status);
    }

    /** Fixed (merged duplicates do not count: they have no resolution of their own). */
    public boolean isResolved() {
        return status != null && RESOLVED.contains(status);
    }

    public boolean hasResolution() {
        return isResolved() && resolutionSummary != null && !resolutionSummary.isBlank();
    }

    public long publicId() {
        return incidentId != null ? incidentId : -docId;
    }

    public SimilarIncident toSimilar(double score) {
        return new SimilarIncident(publicId(), code, title, status, Vectors.round3(score), resolutionSummary);
    }
}
