package in.ac.bitspilani.triagegraph.sla.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import in.ac.bitspilani.triagegraph.shared.contracts.events.PriorityAssigned;
import in.ac.bitspilani.triagegraph.sla.infrastructure.SlaClockRepository;

@Component
public class SlaPriorityChangeListener {

    private final SlaClockRepository clocks;

    public SlaPriorityChangeListener(SlaClockRepository clocks) {
        this.clocks = clocks;
    }

    @Transactional
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)
    public void onPriorityAssigned(PriorityAssigned event) {
        clocks.findByIncidentIdForUpdate(event.incidentId()).ifPresent(clock -> {
            clock.applyPriorityChange(event.current());
            clocks.save(clock);
        });
    }
}
