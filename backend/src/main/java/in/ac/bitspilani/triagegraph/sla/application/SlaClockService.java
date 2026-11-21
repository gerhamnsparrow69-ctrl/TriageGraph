package in.ac.bitspilani.triagegraph.sla.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.SlaTargets;
import in.ac.bitspilani.triagegraph.shared.contracts.events.DomainEvent;
import in.ac.bitspilani.triagegraph.shared.contracts.events.SlaBreached;
import in.ac.bitspilani.triagegraph.shared.contracts.events.SlaWarningRaised;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.DomainEventPublisher;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.SlaService;
import in.ac.bitspilani.triagegraph.sla.domain.SlaClock;
import in.ac.bitspilani.triagegraph.sla.infrastructure.SlaClockRepository;

@Service
public class SlaClockService {

    private final SlaService slaPolicy;
    private final SlaClockRepository clocks;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;
    private final ConcurrentMap<Long, ReentrantLock> incidentLocks = new ConcurrentHashMap<>();

    public SlaClockService(SlaService slaPolicy, SlaClockRepository clocks,
                           DomainEventPublisher eventPublisher, Clock clock) {
        this.slaPolicy = slaPolicy;
        this.clocks = clocks;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Transactional
    public void process(IncidentSnapshot incident) {
        ReentrantLock incidentLock = incidentLocks.computeIfAbsent(incident.id(), ignored -> new ReentrantLock());
        incidentLock.lock();
        try {
            Instant now = Instant.now(clock);
            SlaClock slaClock = clocks.findByIncidentIdForUpdate(incident.id())
                    .orElseGet(() -> new SlaClock(incident, now));
            slaClock.update(incident, now);

            List<DomainEvent> events = evaluate(slaClock, incident, now);
            clocks.saveAndFlush(slaClock);
            publishAfterCommit(events);
        } finally {
            incidentLock.unlock();
            incidentLocks.remove(incident.id(), incidentLock);
        }
    }

    private void publishAfterCommit(List<DomainEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            eventPublisher.publishAll(events);
                        }
                    });
        } else {
            eventPublisher.publishAll(events);
        }
    }

    private List<DomainEvent> evaluate(SlaClock slaClock, IncidentSnapshot incident, Instant now) {
        SlaTargets targets = slaPolicy.targetsFor(incident.serviceId(), incident.priority());
        List<DomainEvent> events = new ArrayList<>();

        events.addAll(evaluateKind(slaClock, incident, now, targets, SlaKind.RESPONSE,
                targets.response()));
        events.addAll(evaluateKind(slaClock, incident, now, targets, SlaKind.RESOLUTION,
                targets.resolution()));
        return events;
    }

    private List<DomainEvent> evaluateKind(SlaClock slaClock, IncidentSnapshot incident, Instant now,
                                          SlaTargets targets, SlaKind kind, Duration target) {
        List<DomainEvent> events = new ArrayList<>();
        long elapsedMillis = slaClock.elapsedMillis(kind, now);
        long warningMillis = target.toMillis() * targets.warningPercent() / 100;
        Instant dueAt = slaClock.dueAt(kind, target, now);

        if (elapsedMillis >= warningMillis && !slaClock.isWarningRaised(kind)) {
            slaClock.markWarningRaised(kind);
            events.add(new SlaWarningRaised(
                    incident.id(), incident.code(), null, now, kind, dueAt));
        }

        if (elapsedMillis >= target.toMillis() && !slaClock.isBreachRaised(kind)) {
            slaClock.markBreachRaised(kind);
            events.add(new SlaBreached(
                    incident.id(), incident.code(), null, now, kind, dueAt));
        }

        return events;
    }
}
