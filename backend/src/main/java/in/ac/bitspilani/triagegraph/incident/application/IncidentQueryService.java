package in.ac.bitspilani.triagegraph.incident.application;

import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.incident.domain.IncidentRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Read-only view of incidents for other modules (technician load, SLA monitor, correlation). */
@Service
@Transactional(readOnly = true)
public class IncidentQueryService implements IncidentQueryPort {

    private final IncidentRepository repository;

    public IncidentQueryService(IncidentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<IncidentSnapshot> find(long id) {
        return repository.findById(id).map(Incident::toSnapshot);
    }

    @Override
    public List<IncidentSnapshot> openSince(Instant since) {
        return repository.findOpenCreatedSince(since).stream().map(Incident::toSnapshot).toList();
    }

    @Override
    public List<IncidentSnapshot> openWithDeadlineBefore(Instant t) {
        return repository.findOpenWithDeadlineBefore(t).stream().map(Incident::toSnapshot).toList();
    }
}
