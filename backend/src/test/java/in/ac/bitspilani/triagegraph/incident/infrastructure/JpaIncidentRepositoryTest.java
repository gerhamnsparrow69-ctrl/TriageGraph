package in.ac.bitspilani.triagegraph.incident.infrastructure;

import in.ac.bitspilani.triagegraph.incident.domain.Actor;
import in.ac.bitspilani.triagegraph.incident.domain.ClassificationInfo;
import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.incident.domain.IncidentFixtures;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.PriorityAssessment;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentAction;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;
import in.ac.bitspilani.triagegraph.shared.contracts.events.NoteAdded;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static in.ac.bitspilani.triagegraph.incident.domain.IncidentFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaIncidentRepository.class)
class JpaIncidentRepositoryTest {

    @Autowired
    JpaIncidentRepository repository;

    @Autowired
    EntityManager em;

    @Test
    void roundTripsTheAggregateAndRebuildsItsState() {
        Incident saved = repository.save(assigned(T0));
        saved.pullEvents();
        saved.addNote("Checked gateway logs", true, TECH, T0.plusSeconds(30));
        saved.addFollower(77);
        repository.save(saved);
        NoteAdded noteEvent = (NoteAdded) saved.pullEvents().getFirst();
        em.clear();

        Incident loaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(noteEvent.noteId()).isPositive();
        assertThat(loaded.getStatus()).isEqualTo(IncidentStatus.ASSIGNED);
        assertThat(loaded.getPriority()).isEqualTo(Priority.P1);
        assertThat(loaded.getClassification().getSource()).isEqualTo("USER");
        assertThat(loaded.sla().getResponseDueAt()).isEqualTo(T0.plus(P1_SLA.response()));
        assertThat(loaded.getNotes()).singleElement().matches(n -> n.isInternal() && n.getId() != null);
        assertThat(loaded.getFollowerIds()).containsExactly(77L);
        assertThat(loaded.getResolution()).isNull();
        assertThat(loaded.allowedActions(TECH)).contains(IncidentAction.ACKNOWLEDGE);   // state rebuilt from status
        loaded.acknowledge(TECH, T0.plusSeconds(60));
        assertThat(loaded.getStatus()).isEqualTo(IncidentStatus.ACKNOWLEDGED);
    }

    @Test
    void openQueriesSkipTerminalIncidentsAndPausedClocks() {
        Instant base = Instant.parse("2030-01-01T00:00:00Z");
        Incident waiting = repository.save(assigned(base));
        Incident acknowledged = assigned(base);
        acknowledged.acknowledge(TECH, base.plusSeconds(60));
        repository.save(acknowledged);
        Incident paused = assigned(base);
        paused.acknowledge(TECH, base.plusSeconds(60));
        paused.startWork(TECH, base.plusSeconds(70));
        paused.resolve("Fixed", null, TECH, base.plusSeconds(80));
        repository.save(paused);
        Incident merged = draft(base);
        merged.mergeInto(waiting, LEAD, base);
        repository.save(merged);

        assertThat(repository.findOpenCreatedSince(base)).extracting(Incident::getId)
                .containsExactly(waiting.getId(), acknowledged.getId(), paused.getId());
        // 20 min in: response (15 min) overdue only for the unacknowledged one
        assertThat(repository.findOpenWithDeadlineBefore(base.plusSeconds(20 * 60))).extracting(Incident::getId)
                .containsExactly(waiting.getId());
        // 5 h in: resolution (4 h) overdue too, but the resolved incident's clock is paused
        assertThat(repository.findOpenWithDeadlineBefore(base.plusSeconds(5 * 3600))).extracting(Incident::getId)
                .containsExactly(waiting.getId(), acknowledged.getId());
    }

    private static Incident draft(Instant at) {
        return Incident.builder().title("Payment failing").description("Gateway timeout").reporterId(REPORTER_ID)
                .impact(Impact.EXTENSIVE).urgency(Urgency.CRITICAL).affectedUsers(300).createdAt(at).build();
    }

    private Incident assigned(Instant at) {
        Incident i = draft(at);
        i.classify("APPLICATION", 1, new ClassificationInfo("USER", 1, "x"), Actor.system(), at);
        i.applyPriority(new PriorityAssessment(Priority.P1, 83.2, "WEIGHTED", "r"), Actor.system(), at);
        i.startSla(IncidentFixtures.P1_SLA, at);
        i.assignTo(TECH_ID, "TEST", Actor.system(), at);
        return i;
    }
}
