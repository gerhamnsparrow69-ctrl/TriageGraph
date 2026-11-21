package in.ac.bitspilani.triagegraph.topology.rca.infrastructure;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.topology.rca.domain.OpenIncidentLookup;

@Component
public class IncidentOpenLookupAdapter implements OpenIncidentLookup {

    private static final Logger log = LoggerFactory.getLogger(IncidentOpenLookupAdapter.class);

    /*
     * Default RCA mapping pending confirmation from M1: in-flight states are open,
     * while RESOLVED, VERIFIED, CLOSED, and MERGED are excluded.
     */
    private static final Set<IncidentStatus> RCA_OPEN_STATUSES = Set.of(
            IncidentStatus.NEW,
            IncidentStatus.CLASSIFIED,
            IncidentStatus.ASSIGNED,
            IncidentStatus.ACKNOWLEDGED,
            IncidentStatus.IN_PROGRESS,
            IncidentStatus.PENDING_INFO);

    private final ObjectProvider<IncidentQueryPort> incidentQueries;
    private final AtomicBoolean missingIntegrationWarningLogged = new AtomicBoolean();

    public IncidentOpenLookupAdapter(ObjectProvider<IncidentQueryPort> incidentQueries) {
        this.incidentQueries = incidentQueries;
    }

    @Override
    public Set<Long> openIncidentServiceIds() {
        IncidentQueryPort incidents = incidentQueries.getIfAvailable();
        if (incidents == null) {
            if (missingIntegrationWarningLogged.compareAndSet(false, true)) {
                log.warn("IncidentQueryPort is unavailable; RCA open-incident evidence bonus is temporarily unavailable");
            }
            return Set.of();
        }

        TreeSet<Long> serviceIds = new TreeSet<>();
        for (IncidentSnapshot incident : incidents.openSince(Instant.EPOCH)) {
            if (incident != null && incident.status() != null
                    && RCA_OPEN_STATUSES.contains(incident.status())) {
                serviceIds.add(incident.serviceId());
            }
        }
        return Collections.unmodifiableSet(serviceIds);
    }
}
