package in.ac.bitspilani.triagegraph.triage.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentReported;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentStatusChanged;

/**
 * Observer: keeps the knowledge base in step with Member 1's incidents using only the shared events. Runs
 * after commit; any failure is logged and never affects the incident itself.
 */
@Component
public class TriageEventListener {

    private static final Logger log = LoggerFactory.getLogger(TriageEventListener.class);

    private final KnowledgeBaseService knowledge;

    public TriageEventListener(KnowledgeBaseService knowledge) {
        this.knowledge = knowledge;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onReported(IncidentReported event) {
        try {
            knowledge.ensureIndexed(event.incidentId());
            knowledge.applyClassification(event.incidentId(), event.categoryCode(), event.serviceId());
        } catch (RuntimeException e) {
            log.warn("Triage could not index {}: {}", event.incidentCode(), e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStatusChanged(IncidentStatusChanged event) {
        try {
            knowledge.applyStatus(event.incidentId(), event.to(), event.reason(), event.occurredAt());
        } catch (RuntimeException e) {
            log.warn("Triage could not record the status of {}: {}", event.incidentCode(), e.getMessage());
        }
    }
}
