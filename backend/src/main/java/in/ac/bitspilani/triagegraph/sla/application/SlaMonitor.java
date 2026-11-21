package in.ac.bitspilani.triagegraph.sla.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;

@Component
public class SlaMonitor {

    private final ObjectProvider<IncidentQueryPort> incidents;
    private final SlaClockService clockService;
    private final Clock clock;

    public SlaMonitor(ObjectProvider<IncidentQueryPort> incidents,
                      SlaClockService clockService, Clock clock) {
        this.incidents = incidents;
        this.clockService = clockService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${app.sla.monitor-delay-ms:30000}")
    public void monitor() {
        IncidentQueryPort incidentQuery = incidents.getIfAvailable();
        if (incidentQuery == null) {
            return;
        }

        for (var incident : incidentQuery.openSince(Instant.EPOCH)) {
            clockService.process(incident);
        }
    }
}
