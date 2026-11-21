package in.ac.bitspilani.triagegraph.reporting.domain;

import static in.ac.bitspilani.triagegraph.reporting.domain.Facts.T0;
import static in.ac.bitspilani.triagegraph.reporting.domain.Facts.incident;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaStatus;

class IncidentFactTest {

    @Test
    void openIncidentIsOnTrackThenAtRiskAt75PercentThenBreached() {
        IncidentFact f = incident(1).build(); // 24 h window from T0
        assertThat(f.slaStatus(T0.plus(Duration.ofHours(10)))).isEqualTo(SlaStatus.ON_TRACK);
        assertThat(f.slaStatus(T0.plus(Duration.ofHours(18)))).isEqualTo(SlaStatus.AT_RISK);
        assertThat(f.slaStatus(T0.plus(Duration.ofHours(25)))).isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    void pausedClockReportsPaused() {
        IncidentFact f = incident(1).status(IncidentStatus.PENDING_INFO).paused(T0.plusSeconds(60)).build();
        assertThat(f.slaStatus(T0.plus(Duration.ofHours(30)))).isEqualTo(SlaStatus.PAUSED);
    }

    @Test
    void resolvedIncidentIsMetOrBreachedByWhenItWasResolved() {
        assertThat(incident(1).resolved(T0.plus(Duration.ofHours(5))).build().slaStatus(T0.plus(Duration.ofDays(9))))
                .isEqualTo(SlaStatus.MET);
        assertThat(incident(2).closed(T0.plus(Duration.ofHours(30))).build().slaStatus(T0.plus(Duration.ofDays(9))))
                .isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    void noTargetCountsAsOnTrackWhileOpen() {
        assertThat(incident(1).due(null).build().slaStatus(T0.plus(Duration.ofDays(30)))).isEqualTo(SlaStatus.ON_TRACK);
    }

    @Test
    void timesToAcknowledgeAndResolve() {
        IncidentFact f = incident(1).responded(T0.plus(Duration.ofMinutes(12))).resolved(T0.plus(Duration.ofHours(3))).build();
        assertThat(f.timeToAcknowledge()).contains(Duration.ofMinutes(12));
        assertThat(f.timeToResolve()).contains(Duration.ofHours(3));
        assertThat(incident(2).build().timeToResolve()).isEmpty();
    }

    @Test
    void openAndResolvedFlags() {
        assertThat(incident(1).build().isOpen()).isTrue();
        assertThat(incident(1).closed(T0.plusSeconds(60)).build().isOpen()).isFalse();
        assertThat(incident(1).status(IncidentStatus.MERGED).build().isOpen()).isFalse();
    }

    @Test
    void involvesReporterFollowersAndAssignee() {
        IncidentFact f = incident(1).reporter(100).assignee(200L).followers(Set.of(300L)).build();
        assertThat(f.involves(100)).isTrue();
        assertThat(f.involves(200)).isTrue();
        assertThat(f.involves(300)).isTrue();
        assertThat(f.involves(400)).isFalse();
    }

    @Test
    void userOverrideIsNotAnAcceptedSuggestion() {
        assertThat(incident(1).source("KNN").build().wasSuggestionAccepted()).isTrue();
        assertThat(incident(1).source("USER").build().wasSuggestionAccepted()).isFalse();
        assertThat(incident(1).source(null).build().wasSuggestionAccepted()).isFalse();
    }
}
