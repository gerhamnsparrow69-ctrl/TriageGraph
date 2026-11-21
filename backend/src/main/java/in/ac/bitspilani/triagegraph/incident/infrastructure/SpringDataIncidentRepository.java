package in.ac.bitspilani.triagegraph.incident.infrastructure;

import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

interface SpringDataIncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatusNotInAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
            Collection<IncidentStatus> excluded, Instant since);

    /** Response clock: due and not yet acknowledged. Resolution clock: due and not paused. */
    @Query("""
            select i from Incident i
            where i.status not in :excluded
              and ((i.sla.responseDueAt < :t and i.sla.respondedAt is null)
                or (i.sla.resolutionDueAt < :t and i.sla.pausedAt is null))
            order by i.createdAt asc""")
    List<Incident> findOpenWithDeadlineBefore(@Param("excluded") Collection<IncidentStatus> excluded,
            @Param("t") Instant t);
}
