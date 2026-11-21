package in.ac.bitspilani.triagegraph.notification.application;

import static in.ac.bitspilani.triagegraph.notification.application.NotificationEvents.*;

import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.notification.domain.NotificationTrigger;
import in.ac.bitspilani.triagegraph.notification.domain.Recipient;
import in.ac.bitspilani.triagegraph.notification.domain.RecipientRole;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.NotificationMessage;

/** Writes the subject and body for each event, worded for the person receiving it. */
@Component
public class NotificationMessageFactory {

    public NotificationMessage create(NotificationTrigger t, Recipient r) {
        String ref = t.incidentCode() == null ? "" : t.incidentCode();
        String title = t.incidentTitle() == null ? "" : " \"" + t.incidentTitle() + "\"";
        String prio = t.priority() == null ? "" : "[" + t.priority() + "] ";
        String subject = prio + switch (t.eventType()) {
            case REPORTED -> ref + " received";
            case ASSIGNED -> r.role() == RecipientRole.ASSIGNEE ? ref + " assigned to you" : ref + " has a technician";
            case PRIORITY_CHANGED -> ref + " priority changed";
            case INFO_REQUESTED -> ref + " needs more information from you";
            case SLA_WARNING -> ref + " is close to its SLA deadline";
            case SLA_BREACHED -> ref + " breached its SLA";
            case ESCALATED -> ref + " escalated to you";
            case RESOLVED -> ref + " resolved";
            case CLOSED -> ref + " closed";
            case MAJOR_DECLARED -> "Major incident " + ref + " declared";
            case TEST -> "Test notification";
            default -> ref + " updated";
        };
        String body = switch (t.eventType()) {
            case REPORTED -> "We've logged your incident" + title + " and will keep you posted.";
            case ASSIGNED -> r.role() == RecipientRole.ASSIGNEE
                    ? "Incident " + ref + title + " is now in your queue. Please acknowledge it."
                    : "A technician is now working on " + ref + title + ".";
            case INFO_REQUESTED -> "The technician on " + ref + title + " has asked a question. Please reply so work can continue.";
            case RESOLVED -> "Incident " + ref + title + " has been marked resolved. Please confirm the fix or reopen it.";
            case TEST -> "If you can read this, this channel works.";
            default -> "Incident " + ref + title + ".";
        };
        if (t.detail() != null && !t.detail().isBlank()) {
            body = body + " " + t.detail();
        }
        return new NotificationMessage(r.user().id(), subject, body, t.priority(), t.eventType(), t.incidentId());
    }
}
