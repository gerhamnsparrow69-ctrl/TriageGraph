package in.ac.bitspilani.triagegraph.notification.application;

import static in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType.EMAIL;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType.IN_APP;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType.PUSH;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.notification.domain.RecipientRole;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

class RuleTableChannelSelectionPolicyTest {

    private final RuleTableChannelSelectionPolicy policy =
            new RuleTableChannelSelectionPolicy(RuleTableChannelSelectionPolicy.DEFAULT_RULES);

    @Test
    void p1AssignmentAlsoEmailsTheAssignee() {
        assertThat(policy.channelsFor(NotificationEvents.ASSIGNED, RecipientRole.ASSIGNEE, Priority.P1))
                .containsExactlyInAnyOrder(IN_APP, PUSH, EMAIL);
        assertThat(policy.channelsFor(NotificationEvents.ASSIGNED, RecipientRole.ASSIGNEE, Priority.P3))
                .containsExactlyInAnyOrder(IN_APP, PUSH);
    }

    @Test
    void reporterOnlyGetsTheBellWhenATechnicianIsAssigned() {
        assertThat(policy.channelsFor(NotificationEvents.ASSIGNED, RecipientRole.REPORTER, Priority.P1))
                .containsExactly(IN_APP);
    }

    @Test
    void breachReachesTheTeamLeadOnEveryChannel() {
        assertThat(policy.channelsFor(NotificationEvents.SLA_BREACHED, RecipientRole.TEAM_LEAD, Priority.P2))
                .containsExactlyInAnyOrder(IN_APP, PUSH, EMAIL);
    }

    @Test
    void unknownCombinationsGetNothing() {
        assertThat(policy.channelsFor(NotificationEvents.CLOSED, RecipientRole.ASSIGNEE, Priority.P1)).isEmpty();
    }

    @Test
    void addingARuleRowIsAllItTakesToRouteSomewhereNew() {
        var custom = new RuleTableChannelSelectionPolicy(List.of(
                NotificationRule.of(NotificationEvents.CLOSED, RecipientRole.ASSIGNEE, PUSH)));
        assertThat(custom.channelsFor(NotificationEvents.CLOSED, RecipientRole.ASSIGNEE, null)).containsExactly(PUSH);
    }
}
