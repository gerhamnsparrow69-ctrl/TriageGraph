package in.ac.bitspilani.triagegraph.incident.application;

import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import org.junit.jupiter.api.Test;

import static in.ac.bitspilani.triagegraph.incident.domain.IncidentFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

class IncidentQueryServiceTest {

    final InMemoryIncidentRepository repo = new InMemoryIncidentRepository();
    final IncidentQueryService query = new IncidentQueryService(repo);

    @Test
    void findsSnapshotsAndSkipsClosedIncidents() {
        Incident open = repo.save(inStatus(IncidentStatus.ASSIGNED));
        repo.save(inStatus(IncidentStatus.CLOSED));

        assertThat(query.find(open.getId())).map(IncidentSnapshot::assigneeId).contains(TECH_ID);
        assertThat(query.find(999)).isEmpty();
        assertThat(query.openSince(T0)).extracting(IncidentSnapshot::id).containsExactly(open.getId());
        assertThat(query.openWithDeadlineBefore(T0.plus(P1_SLA.resolution()).plusSeconds(1)))
                .extracting(IncidentSnapshot::id).containsExactly(open.getId());
    }
}
