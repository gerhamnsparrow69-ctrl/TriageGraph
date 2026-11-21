package in.ac.bitspilani.triagegraph.notification.application;

import java.util.Set;

import in.ac.bitspilani.triagegraph.notification.domain.RecipientRole;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/** Strategy: decides which channels a recipient gets for an event. */
public interface ChannelSelectionPolicy {

    Set<ChannelType> channelsFor(String eventType, RecipientRole role, Priority priority);
}
