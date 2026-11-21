package in.ac.bitspilani.triagegraph.shared.contracts.ports;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.SlaTargets;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/** Provided by Member 3 (sla). */
public interface SlaService {

    /** @throws in.ac.bitspilani.triagegraph.shared.exception.SlaConfigurationMissingException no policy applies */
    SlaTargets targetsFor(long serviceId, Priority p);
}
