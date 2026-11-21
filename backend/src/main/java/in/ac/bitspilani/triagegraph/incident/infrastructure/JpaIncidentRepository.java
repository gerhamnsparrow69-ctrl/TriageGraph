package in.ac.bitspilani.triagegraph.incident.infrastructure;

import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.incident.domain.IncidentRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
class JpaIncidentRepository implements IncidentRepository {

    /** Terminal statuses; everything else counts as open. */
    static final Set<IncidentStatus> TERMINAL = EnumSet.of(IncidentStatus.CLOSED, IncidentStatus.MERGED);

    private final SpringDataIncidentRepository jpa;
    private final EntityManager em;

    JpaIncidentRepository(SpringDataIncidentRepository jpa, EntityManager em) {
        this.jpa = jpa;
        this.em = em;
    }

    /**
     * New incidents are persisted; managed ones are only flushed. Spring Data's save() would call merge()
     * on a managed incident, and merge cascades to new notes by persisting <em>copies</em>, leaving the
     * notes the aggregate (and its pending NoteAdded events) holds without ids. Flushing cascades PERSIST
     * to the original note objects instead.
     */
    @Override
    public Incident save(Incident incident) {
        if (incident.getId() != null && em.contains(incident)) {
            em.flush();
            return incident;
        }
        if (incident.getId() != null && incident.hasPendingEvents()) {
            throw new IllegalStateException("Load " + incident.getCode() + " in this transaction before changing it;"
                    + " merging a detached copy would drop its pending events.");
        }
        return jpa.saveAndFlush(incident);
    }

    @Override
    public Optional<Incident> findById(long id) {
        return jpa.findById(id);
    }

    @Override
    public List<Incident> findOpenCreatedSince(Instant since) {
        return jpa.findByStatusNotInAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(TERMINAL, since);
    }

    @Override
    public List<Incident> findOpenWithDeadlineBefore(Instant t) {
        return jpa.findOpenWithDeadlineBefore(TERMINAL, t);
    }
}
