package in.ac.bitspilani.triagegraph.notification.application;

import java.util.Set;

import in.ac.bitspilani.triagegraph.notification.domain.RecipientRole;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/**
 * One row of the routing table: for this event and recipient role, use these channels.
 * {@code onlyFor} limits the row to one priority (e.g. email on assignment only for P1); null means any.
 */
public record NotificationRule(String eventType, RecipientRole role, Priority onlyFor, Set<ChannelType> channels) {

    public static NotificationRule of(String eventType, RecipientRole role, ChannelType... channels) {
        return new NotificationRule(eventType, role, null, Set.of(channels));
    }

    public static NotificationRule ifPriority(Priority p, String eventType, RecipientRole role, ChannelType... channels) {
        return new NotificationRule(eventType, role, p, Set.of(channels));
    }

    boolean matches(String event, RecipientRole r, Priority p) {
        return eventType.equals(event) && role == r && (onlyFor == null || onlyFor == p);
    }
}
