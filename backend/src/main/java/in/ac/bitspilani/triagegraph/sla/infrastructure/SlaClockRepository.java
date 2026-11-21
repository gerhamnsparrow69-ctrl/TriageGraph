package in.ac.bitspilani.triagegraph.sla.infrastructure;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import in.ac.bitspilani.triagegraph.sla.domain.SlaClock;

public interface SlaClockRepository extends JpaRepository<SlaClock, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from SlaClock c where c.incidentId = :incidentId")
    Optional<SlaClock> findByIncidentIdForUpdate(@Param("incidentId") Long incidentId);

    @Query("""
            select count(case when c.responseBreachRaised = true then c.incidentId end) as responseBreachCount,
                   count(case when c.resolutionBreachRaised = true then c.incidentId end) as resolutionBreachCount,
                   count(distinct case
                       when c.responseBreachRaised = true or c.resolutionBreachRaised = true
                       then c.incidentId end) as incidentsWithBreaches
            from SlaClock c
            """)
    SlaBreachCounts countBreachCounts();

    interface SlaBreachCounts {

        Long getResponseBreachCount();

        Long getResolutionBreachCount();

        Long getIncidentsWithBreaches();
    }
}
