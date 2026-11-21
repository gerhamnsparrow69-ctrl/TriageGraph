package in.ac.bitspilani.triagegraph.shared.contracts.ports;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Provided by Member 1 (incident). */
public interface IncidentQueryPort {

    Optional<IncidentSnapshot> find(long id);

    List<IncidentSnapshot> openSince(Instant since);

    List<IncidentSnapshot> openWithDeadlineBefore(Instant t);
}
