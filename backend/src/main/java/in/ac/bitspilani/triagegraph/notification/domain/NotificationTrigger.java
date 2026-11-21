package in.ac.bitspilani.triagegraph.notification.domain;

import java.util.List;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/**
 * A domain event translated into notification terms: which kind of event, about which
 * incident, and who should hear about it.
 */
public record NotificationTrigger(String eventType, Priority priority, Long incidentId, String incidentCode,
        String incidentTitle, String detail, List<Recipient> recipients) {
}
