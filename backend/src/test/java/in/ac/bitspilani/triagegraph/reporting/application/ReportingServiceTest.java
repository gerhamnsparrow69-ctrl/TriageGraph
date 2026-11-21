package in.ac.bitspilani.triagegraph.reporting.application;

import static in.ac.bitspilani.triagegraph.reporting.domain.Facts.T0;
import static in.ac.bitspilani.triagegraph.reporting.domain.Facts.incident;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.reporting.application.Reports.Dashboard;
import in.ac.bitspilani.triagegraph.reporting.domain.ServiceFact;
import in.ac.bitspilani.triagegraph.reporting.domain.TechnicianFact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

class ReportingServiceTest {

    /** 2 days after T0. */
    private static final Instant NOW = T0.plus(Duration.ofDays(2));

    private final FakeReadModel data = new FakeReadModel();
    private final ReportingService service = new ReportingService(data, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void data() {
        data.services.add(new ServiceFact(1, "Hostel Wi-Fi", "HIGH"));
        data.services.add(new ServiceFact(2, "Campus SSO", "MISSION_CRITICAL"));
        data.services.add(new ServiceFact(3, "Print Services", "LOW"));
        data.technicians.add(new TechnicianFact(200, "Ravi", "TECHNICIAN", 6, true, 300L));
        data.technicians.add(new TechnicianFact(201, "Sneha", "TECHNICIAN", 4, false, 300L));
        // 1: open P1 on SSO, past due -> breached, critical open, service outage
        data.incidents.add(incident(1).priority(Priority.P1).service(2L).category("AUTHENTICATION").assignee(200L)
                .due(T0.plus(Duration.ofHours(4))).responded(T0.plus(Duration.ofMinutes(10))).build());
        // 2: resolved in 2 h, within SLA
        data.incidents.add(incident(2).service(1L).assignee(200L).responded(T0.plus(Duration.ofMinutes(20)))
                .resolved(T0.plus(Duration.ofHours(2))).rootCause("AP firmware").followers(Set.of(9L, 10L)).build());
        // 3: closed after 30 h against a 24 h target -> breached
        data.incidents.add(incident(3).service(1L).assignee(201L).source("USER").rootCause("AP firmware")
                .closed(T0.plus(Duration.ofHours(30))).build());
        // 4: merged duplicate, part of a major incident
        data.incidents.add(incident(4).service(1L).status(IncidentStatus.MERGED).assignee(null).major(77L).build());
    }

    @Test
    void overviewCountsAndTimes() {
        Reports.Overview o = service.overview();
        assertThat(o.total()).isEqualTo(4);
        assertThat(o.open()).isEqualTo(1);
        assertThat(o.criticalOpen()).isEqualTo(1);
        assertThat(o.resolved()).isEqualTo(1);
        assertThat(o.closed()).isEqualTo(1);
        assertThat(o.slaViolations()).isEqualTo(2); // the open P1 and the late close
        assertThat(o.slaCompliancePercent()).isEqualTo(33.3); // 1 met of 3 judged; the merged duplicate is left out
        assertThat(o.mttaMinutes()).isEqualTo(15.0);
        assertThat(o.mttrMinutes()).isEqualTo(960.0); // (2 h + 30 h) / 2
    }

    @Test
    void technicianWorkloadBusiestFirst() {
        Dashboard d = service.dashboard();
        assertThat(d.technicians()).extracting(Reports.TechnicianWorkload::name).containsExactly("Ravi", "Sneha");
        Reports.TechnicianWorkload ravi = d.technicians().getFirst();
        assertThat(ravi.active()).isEqualTo(1);
        assertThat(ravi.resolvedLast7Days()).isEqualTo(1);
        assertThat(ravi.avgResolutionMinutes()).isEqualTo(120.0);
        assertThat(d.technicians().get(1).available()).isFalse();
    }

    @Test
    void serviceAnalysisWithHealthAndRootCauses() {
        Dashboard d = service.dashboard();
        Reports.ServiceAnalysis wifi = d.services().getFirst();
        assertThat(wifi.name()).isEqualTo("Hostel Wi-Fi");
        assertThat(wifi.incidents()).isEqualTo(3);
        assertThat(wifi.health()).isEqualTo("HEALTHY");
        assertThat(wifi.topRootCauses()).containsExactly("AP firmware");
        assertThat(d.services().get(1).health()).isEqualTo("OUTAGE");
        assertThat(d.services().get(2).incidents()).isZero();
    }

    @Test
    void trendHasFourteenDaysEndingTodayInCampusTime() {
        Reports.Trend t = service.dashboard().trend();
        assertThat(t.days()).hasSize(ReportingService.TREND_DAYS);
        assertThat(t.days().getLast().date()).isEqualTo(LocalDate.of(2026, 11, 22));
        Reports.TrendDay reportDay = t.days().stream().filter(d -> d.date().equals(LocalDate.of(2026, 11, 20)))
                .findFirst().orElseThrow();
        assertThat(reportDay.total()).isEqualTo(4);
        assertThat(reportDay.counts()).containsEntry("NETWORK", 3L).containsEntry("AUTHENTICATION", 1L);
        assertThat(t.categories()).containsExactly("AUTHENTICATION", "NETWORK");
    }

    @Test
    void slaByPriority() {
        var rows = service.dashboard().slaByPriority();
        assertThat(rows).hasSize(4);
        assertThat(rows.getFirst()).isEqualTo(new Reports.SlaByPriority("P1", 0, 1, 0));
        assertThat(rows.get(2)).isEqualTo(new Reports.SlaByPriority("P3", 1, 1, 0));
    }

    @Test
    void aiEffectiveness() {
        Reports.AiEffectiveness ai = service.dashboard().ai();
        assertThat(ai.classified()).isEqualTo(4);
        assertThat(ai.suggestionAcceptancePercent()).isEqualTo(75.0);
        assertThat(ai.duplicatesJoined()).isEqualTo(2);
        assertThat(ai.duplicatesMerged()).isEqualTo(1);
        assertThat(ai.ticketsAvoided()).isEqualTo(3);
        assertThat(ai.majorIncidents()).isEqualTo(1);
    }

    @Test
    void emptySystemGivesZerosAndNoAverages() {
        data.incidents.clear();
        Reports.Overview o = service.overview();
        assertThat(o.total()).isZero();
        assertThat(o.slaCompliancePercent()).isNull();
        assertThat(o.mttrMinutes()).isNull();
        assertThat(service.dashboard().ai().suggestionAcceptancePercent()).isNull();
    }
}
