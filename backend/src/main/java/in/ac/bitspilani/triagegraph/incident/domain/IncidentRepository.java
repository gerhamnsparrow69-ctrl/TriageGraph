package in.ac.bitspilani.triagegraph.incident.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Domain view of incident storage; the Spring Data implementation lives in infrastructure. */
public interface IncidentRepository {

    /** Saves and flushes, so generated ids (incident, notes) exist before events are pulled. */
    Incident save(Incident incident);

    Optional<Incident> findById(long id);

    /** Incidents that are not CLOSED or MERGED, created at or after {@code since}. */
    List<Incident> findOpenCreatedSince(Instant since);

    /** Open incidents whose response or resolution deadline is before {@code t}. */
    List<Incident> findOpenWithDeadlineBefore(Instant t);
}
