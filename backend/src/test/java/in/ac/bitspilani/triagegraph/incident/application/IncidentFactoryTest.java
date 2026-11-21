package in.ac.bitspilani.triagegraph.incident.application;

import in.ac.bitspilani.triagegraph.incident.domain.Incident;
import in.ac.bitspilani.triagegraph.incident.domain.IncidentFixtures;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentReported;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentFactoryTest {

    static final Instant NOW = Instant.parse("2026-11-20T09:00:00Z");
    final IncidentFactory factory = new IncidentFactory(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsANewIncidentStampedWithTheClock() {
        Incident i = factory.create(new ReportIncidentCommand("  Wi-Fi down  ", "Block 5 has no Wi-Fi",
                Impact.SIGNIFICANT, Urgency.HIGH, 80, null, null), 7);

        assertThat(i.getStatus()).isEqualTo(IncidentStatus.NEW);
        assertThat(i.getTitle()).isEqualTo("Wi-Fi down");
        assertThat(i.getReporterId()).isEqualTo(7);
        assertThat(i.getAffectedUsers()).isEqualTo(80);
        assertThat(i.getCreatedAt()).isEqualTo(NOW);
        IncidentFixtures.giveId(i, 1);
        IncidentReported e = (IncidentReported) i.pullEvents().getFirst();
        assertThat(e.actorId()).isEqualTo(7);
        assertThat(e.occurredAt()).isEqualTo(NOW);
    }

    @Test
    void canBackdateForSeeding() {
        Instant earlier = NOW.minusSeconds(240);
        Incident i = factory.create(new ReportIncidentCommand("t", "d", Impact.MINOR, Urgency.LOW, 1, null, null),
                7, earlier);

        assertThat(i.getCreatedAt()).isEqualTo(earlier);
    }

    @Test
    void userChoiceNeedsBothCategoryAndService() {
        assertThat(new ReportIncidentCommand("t", "d", Impact.MINOR, Urgency.LOW, 1, "NETWORK", 3L).hasUserChoice())
                .isTrue();
        assertThat(new ReportIncidentCommand("t", "d", Impact.MINOR, Urgency.LOW, 1, "NETWORK", null).hasUserChoice())
                .isFalse();
        assertThat(new ReportIncidentCommand("t", "d", Impact.MINOR, Urgency.LOW, 1, " ", 3L).hasUserChoice())
                .isFalse();
    }
}
