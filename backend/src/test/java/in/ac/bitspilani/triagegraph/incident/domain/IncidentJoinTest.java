package in.ac.bitspilani.triagegraph.incident.domain;

import static in.ac.bitspilani.triagegraph.incident.domain.IncidentFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.events.DuplicateLinked;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.Test;

/** TC29 (incident side): joining a likely duplicate instead of opening a new ticket. */
class IncidentJoinTest {

    @Test
    void joiningAddsAFollowerAndOneMoreAffectedUser() {
        Incident i = inStatus(IncidentStatus.ASSIGNED);
        giveId(i, 7L);
        int before = i.getAffectedUsers();

        assertThat(i.joinAsAffected(11L, OTHER_EMPLOYEE, T0.plusSeconds(60))).isTrue();

        assertThat(i.getFollowerIds()).contains(11L);
        assertThat(i.getAffectedUsers()).isEqualTo(before + 1);
        assertThat(i.pullEvents()).singleElement().isInstanceOfSatisfying(DuplicateLinked.class, e -> {
            assertThat(e.userId()).isEqualTo(11L);
            assertThat(e.duplicateOfId()).isEqualTo(7L);
            assertThat(e.merged()).isFalse();
        });
    }

    @Test
    void theReporterOrAnExistingFollowerChangesNothing() {
        Incident i = inStatus(IncidentStatus.ASSIGNED);
        giveId(i, 7L);
        i.joinAsAffected(11L, OTHER_EMPLOYEE, T0);
        i.pullEvents();
        int count = i.getAffectedUsers();

        assertThat(i.joinAsAffected(11L, OTHER_EMPLOYEE, T0)).isFalse();
        assertThat(i.joinAsAffected(REPORTER_ID, REPORTER, T0)).isFalse();
        assertThat(i.getAffectedUsers()).isEqualTo(count);
        assertThat(i.pullEvents()).isEmpty();
    }

    @Test
    void aClosedIncidentCannotBeJoined() {
        Incident i = inStatus(IncidentStatus.CLOSED);
        giveId(i, 7L);
        assertThatThrownBy(() -> i.joinAsAffected(11L, OTHER_EMPLOYEE, T0))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
