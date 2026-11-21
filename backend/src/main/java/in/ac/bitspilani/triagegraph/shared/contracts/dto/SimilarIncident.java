package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;

public record SimilarIncident(
        long incidentId, String code, String title, IncidentStatus status,
        double score, String resolutionSummary) {
}
