package in.ac.bitspilani.triagegraph.shared.contracts.dto;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

public record NotificationMessage(
        long recipientId, String subject, String body, Priority priority,
        String eventType, Long incidentId) {
}
