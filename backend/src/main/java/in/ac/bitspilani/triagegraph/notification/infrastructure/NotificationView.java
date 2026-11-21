package in.ac.bitspilani.triagegraph.notification.infrastructure;

import java.time.Instant;

import in.ac.bitspilani.triagegraph.notification.domain.Notification;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/** JSON shape of an inbox item, shared by the REST API and the live stream. */
public record NotificationView(long id, String subject, String body, Priority priority, String eventType,
        Long incidentId, Instant createdAt, boolean read) {

    public static NotificationView from(Notification n) {
        return new NotificationView(n.getId(), n.getSubject(), n.getBody(), n.getPriority(), n.getEventType(),
                n.getIncidentId(), n.getCreatedAt(), n.isRead());
    }
}
