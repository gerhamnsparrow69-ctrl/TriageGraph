package in.ac.bitspilani.triagegraph.reporting.domain;

import static in.ac.bitspilani.triagegraph.reporting.domain.Facts.T0;
import static in.ac.bitspilani.triagegraph.reporting.domain.Facts.incident;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaStatus;

class IncidentSpecificationsTest {

    private final IncidentFact wifi = incident(4).title("Hostel Wi-Fi keeps dropping").reporter(100).assignee(200L)
            .followers(Set.of(101L)).build();

    @Test
    void codeMatchesFullCodeNumberOrId() {
        assertThat(IncidentSpecifications.code("INC-1004").isSatisfiedBy(wifi)).isTrue();
        assertThat(IncidentSpecifications.code("inc-1004").isSatisfiedBy(wifi)).isTrue();
        assertThat(IncidentSpecifications.code("1004").isSatisfiedBy(wifi)).isTrue();
        assertThat(IncidentSpecifications.code("4").isSatisfiedBy(wifi)).isTrue();
        assertThat(IncidentSpecifications.code("INC-1005").isSatisfiedBy(wifi)).isFalse();
    }

    @Test
    void textNeedsEveryWordInAnyOrder() {
        assertThat(IncidentSpecifications.text("wi-fi hostel").isSatisfiedBy(wifi)).isTrue();
        assertThat(IncidentSpecifications.text("wi-fi printer").isSatisfiedBy(wifi)).isFalse();
    }

    @Test
    void dateRangeIsFromInclusiveToExclusive() {
        assertThat(IncidentSpecifications.reportedFrom(T0).isSatisfiedBy(wifi)).isTrue();
        assertThat(IncidentSpecifications.reportedBefore(T0).isSatisfiedBy(wifi)).isFalse();
    }

    @Test
    void rulesCombineWithAnd() {
        IncidentSpecification spec = IncidentSpecifications.statusIn(Set.of(IncidentStatus.ASSIGNED))
                .and(IncidentSpecifications.priorityIn(Set.of(Priority.P3)))
                .and(IncidentSpecifications.category("network"))
                .and(IncidentSpecifications.service(1))
                .and(IncidentSpecifications.assignedTo(200))
                .and(IncidentSpecifications.reportedBy(100))
                .and(IncidentSpecifications.majorIncident(false))
                .and(IncidentSpecifications.slaStatus(SlaStatus.ON_TRACK, T0.plus(Duration.ofHours(1))));
        assertThat(spec.isSatisfiedBy(wifi)).isTrue();
        assertThat(spec.and(IncidentSpecifications.assignedTo(201)).isSatisfiedBy(wifi)).isFalse();
    }

    @Test
    void employeesSeeOwnAndJoinedIncidentsOnly() {
        assertThat(IncidentSpecifications.visibleTo(Viewer.of(100, Role.EMPLOYEE)).isSatisfiedBy(wifi)).isTrue();
        assertThat(IncidentSpecifications.visibleTo(Viewer.of(101, Role.EMPLOYEE)).isSatisfiedBy(wifi)).isTrue();
        assertThat(IncidentSpecifications.visibleTo(Viewer.of(102, Role.EMPLOYEE)).isSatisfiedBy(wifi)).isFalse();
    }

    @Test
    void techniciansSeeTheirOwnTeamLeadsTheirTeamsAdminsEverything() {
        assertThat(IncidentSpecifications.visibleTo(Viewer.of(200, Role.TECHNICIAN)).isSatisfiedBy(wifi)).isTrue();
        assertThat(IncidentSpecifications.visibleTo(Viewer.of(201, Role.TECHNICIAN)).isSatisfiedBy(wifi)).isFalse();
        assertThat(IncidentSpecifications.visibleTo(new Viewer(300, Role.TEAM_LEAD, Set.of(200L))).isSatisfiedBy(wifi)).isTrue();
        assertThat(IncidentSpecifications.visibleTo(new Viewer(300, Role.TEAM_LEAD, Set.of(201L))).isSatisfiedBy(wifi)).isFalse();
        assertThat(IncidentSpecifications.visibleTo(Viewer.of(1, Role.ADMIN)).isSatisfiedBy(wifi)).isTrue();
        assertThat(IncidentSpecifications.visibleTo(Viewer.of(1, Role.IT_MANAGER)).isSatisfiedBy(wifi)).isTrue();
    }
}
