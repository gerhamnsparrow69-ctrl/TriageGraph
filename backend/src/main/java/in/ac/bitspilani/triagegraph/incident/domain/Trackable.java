package in.ac.bitspilani.triagegraph.incident.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/**
 * Anything with a code, status and priority that people track. A single {@link Incident} and,
 * later, a MajorIncident that groups many incidents are treated the same way (Composite).
 */
public interface Trackable {

    Long getId();

    String getCode();

    IncidentStatus getStatus();

    Priority getPriority();
}
