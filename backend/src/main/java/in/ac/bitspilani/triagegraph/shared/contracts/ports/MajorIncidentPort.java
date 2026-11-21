package in.ac.bitspilani.triagegraph.shared.contracts.ports;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.MajorIncidentDraft;
import java.util.Optional;

/** Provided by Member 1 (incident). */
public interface MajorIncidentPort {

    long declare(MajorIncidentDraft d);

    void attach(long majorId, long incidentId);

    Optional<Long> openMajorFor(long rootCauseServiceId);
}
