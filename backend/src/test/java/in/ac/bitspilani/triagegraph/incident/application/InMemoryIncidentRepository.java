package in.ac.bitspilani.triagegraph.incident.application;

import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.incident.domain.IncidentFixtures;
import in.ac.bitspilani.triagegraph.incident.domain.IncidentRepository;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test double: assigns ids like the database would and keeps incidents in a map. */
class InMemoryIncidentRepository implements IncidentRepository {

    final Map<Long, Incident> store = new LinkedHashMap<>();
    private long nextId = 1;
    int saves;

    @Override
    public Incident save(Incident incident) {
        if (incident.getId() == null) {
            IncidentFixtures.giveId(incident, nextId++);
        }
        IncidentFixtures.simulateSave(incident);
        store.put(incident.getId(), incident);
        saves++;
        return incident;
    }

    @Override
    public Optional<Incident> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Incident> findOpenCreatedSince(Instant since) {
        return store.values().stream().filter(i -> !i.isTerminal() && !i.getCreatedAt().isBefore(since)).toList();
    }

    @Override
    public List<Incident> findOpenWithDeadlineBefore(Instant t) {
        return store.values().stream().filter(i -> !i.isTerminal() && i.sla().getResolutionDueAt() != null
                && i.sla().getResolutionDueAt().isBefore(t)).toList();
    }
}
