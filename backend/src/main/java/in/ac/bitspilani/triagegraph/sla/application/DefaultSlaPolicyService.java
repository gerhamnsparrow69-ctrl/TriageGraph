package in.ac.bitspilani.triagegraph.sla.application;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.SlaTargets;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.SlaService;
import in.ac.bitspilani.triagegraph.shared.exception.SlaConfigurationMissingException;

@Service
public class DefaultSlaPolicyService implements SlaService {

    private static final int WARNING_PERCENT = 75;

    private static final Map<Priority, SlaTargets> DEFAULT_TARGETS;

    static {
        Map<Priority, SlaTargets> targets = new EnumMap<>(Priority.class);
        targets.put(Priority.P1, new SlaTargets(Duration.ofMinutes(5), Duration.ofMinutes(30),
                WARNING_PERCENT, "DEFAULT_P1"));
        targets.put(Priority.P2, new SlaTargets(Duration.ofMinutes(15), Duration.ofHours(2),
                WARNING_PERCENT, "DEFAULT_P2"));
        targets.put(Priority.P3, new SlaTargets(Duration.ofHours(1), Duration.ofHours(8),
                WARNING_PERCENT, "DEFAULT_P3"));
        targets.put(Priority.P4, new SlaTargets(Duration.ofHours(4), Duration.ofHours(24),
                WARNING_PERCENT, "DEFAULT_P4"));
        DEFAULT_TARGETS = Map.copyOf(targets);
    }

    private final CatalogQuery catalog;

    public DefaultSlaPolicyService(CatalogQuery catalog) {
        this.catalog = catalog;
    }

    @Override
    public SlaTargets targetsFor(long serviceId, Priority priority) {
        if (priority == null) {
            throw new SlaConfigurationMissingException("No SLA policy applies to a null priority");
        }

        SlaTargets defaults = DEFAULT_TARGETS.get(priority);
        if (defaults == null) {
            throw new SlaConfigurationMissingException("No default SLA policy applies to " + priority);
        }

        String serviceName = catalog.service(serviceId).name();
        if (priority == Priority.P1 && "Fee Payment App".equals(serviceName)) {
            return new SlaTargets(Duration.ofMinutes(3), Duration.ofMinutes(20),
                    WARNING_PERCENT, "FEE_PAYMENT_APP_P1");
        }

        return defaults;
    }
}
