package in.ac.bitspilani.triagegraph.incident.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.incident.domain.IncidentFixtures;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
import in.ac.bitspilani.triagegraph.shared.contracts.events.DomainEvent;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentEscalated;
import in.ac.bitspilani.triagegraph.shared.contracts.events.SlaBreached;
import in.ac.bitspilani.triagegraph.shared.contracts.events.SlaWarningRaised;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.BlastRadiusProvider;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.PriorityService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** The SLA side of IncidentCommandPort: load, record on the aggregate, save, publish. */
class IncidentCommandServiceSlaTest {

    static final Instant NOW = Instant.parse("2026-11-20T10:00:00Z");

    final InMemoryIncidentRepository repo = new InMemoryIncidentRepository();
    final List<DomainEvent> published = new ArrayList<>();
    IncidentCommandService service;
    long id;

    @BeforeEach
    void setUp() {
        service = new IncidentCommandService(repo, mock(IncidentLifecycleService.class), mock(CatalogQuery.class),
                mock(PriorityService.class), mock(BlastRadiusProvider.class), mock(UserDirectory.class),
                published::addAll, Clock.fixed(NOW, ZoneOffset.UTC));
        Incident incident = IncidentFixtures.inStatus(IncidentStatus.ASSIGNED);
        repo.save(incident);
        id = incident.getId();
    }

    @Test
    void warningIsSavedAndPublished() {
        service.markSlaWarning(id, SlaKind.RESPONSE);

        Incident saved = repo.store.get(id);
        assertThat(saved.getUpdatedAt()).isEqualTo(NOW);
        assertThat(published).singleElement().isInstanceOfSatisfying(SlaWarningRaised.class, e -> {
            assertThat(e.kind()).isEqualTo(SlaKind.RESPONSE);
            assertThat(e.dueAt()).isEqualTo(saved.sla().getResponseDueAt());
            assertThat(e.occurredAt()).isEqualTo(NOW);
        });
    }

    @Test
    void breachIsSavedAndPublished() {
        service.markSlaBreached(id, SlaKind.RESOLUTION);

        assertThat(published).singleElement().isInstanceOfSatisfying(SlaBreached.class, e ->
                assertThat(e.dueAt()).isEqualTo(repo.store.get(id).sla().getResolutionDueAt()));
    }

    @Test
    void escalationUpdatesTheLevelAndPublishes() {
        service.escalate(id, 1, 30, "Response SLA breached");

        assertThat(repo.store.get(id).getEscalationLevel()).isEqualTo(1);
        assertThat(published).singleElement().isInstanceOfSatisfying(IncidentEscalated.class, e -> {
            assertThat(e.level()).isEqualTo(1);
            assertThat(e.targetUserId()).isEqualTo(30);
            assertThat(e.reason()).isEqualTo("Response SLA breached");
        });
    }

    @Test
    void invalidLevelOrUnknownIncidentPublishesNothing() {
        assertThatThrownBy(() -> service.escalate(id, 0, 30, "x")).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> service.markSlaWarning(999, SlaKind.RESPONSE))
                .isInstanceOf(IncidentNotFoundException.class);
        assertThat(published).isEmpty();
    }
}
