package in.ac.bitspilani.triagegraph.assignment.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentStatusChanged;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;

/**
 * Observer: when an incident is resolved, credit the assignee with a fix on that service, so future smart
 * recommendations prefer people who have fixed this service before. Runs after commit; a failure here is
 * logged and never affects the incident itself.
 */
@Component
public class AssignmentEventListener {

    private static final Logger log = LoggerFactory.getLogger(AssignmentEventListener.class);

    private final ObjectProvider<IncidentQueryPort> incidents;
    private final ExperienceService experience;

    public AssignmentEventListener(ObjectProvider<IncidentQueryPort> incidents, ExperienceService experience) {
        this.incidents = incidents;
        this.experience = experience;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStatusChanged(IncidentStatusChanged event) {
        if (event.to() != IncidentStatus.RESOLVED) {
            return;
        }
        try {
            IncidentQueryPort port = incidents.getIfAvailable();
            if (port == null) {
                return;
            }
            port.find(event.incidentId())
                    .filter(s -> s.assigneeId() != null)
                    .ifPresent(s -> experience.recordFix(s.assigneeId(), s.serviceId(), null, s.code(),
                            event.occurredAt()));
        } catch (RuntimeException e) {
            log.warn("Could not record the fix for {}: {}", event.incidentCode(), e.getMessage());
        }
    }
}
