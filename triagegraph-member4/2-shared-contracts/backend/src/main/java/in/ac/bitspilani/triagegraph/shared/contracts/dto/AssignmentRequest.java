package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

public record AssignmentRequest(
        long incidentId, String categoryCode, long serviceId, Priority priority) {
}
