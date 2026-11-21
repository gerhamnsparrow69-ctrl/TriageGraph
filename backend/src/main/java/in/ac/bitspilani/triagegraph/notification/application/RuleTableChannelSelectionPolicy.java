package in.ac.bitspilani.triagegraph.notification.application;

import static in.ac.bitspilani.triagegraph.notification.application.NotificationEvents.*;
import static in.ac.bitspilani.triagegraph.notification.application.NotificationRule.ifPriority;
import static in.ac.bitspilani.triagegraph.notification.application.NotificationRule.of;
import static in.ac.bitspilani.triagegraph.notification.domain.RecipientRole.*;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType.EMAIL;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType.IN_APP;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType.PUSH;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import in.ac.bitspilani.triagegraph.notification.domain.RecipientRole;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/** The routing table from blueprint §10.1. All matching rows are combined. */
public class RuleTableChannelSelectionPolicy implements ChannelSelectionPolicy {

    public static final List<NotificationRule> DEFAULT_RULES = List.of(
            of(REPORTED, REPORTER, IN_APP, EMAIL),
            of(ASSIGNED, ASSIGNEE, IN_APP, PUSH),
            ifPriority(Priority.P1, ASSIGNED, ASSIGNEE, EMAIL),
            of(ASSIGNED, REPORTER, IN_APP),
            of(PRIORITY_CHANGED, ASSIGNEE, IN_APP),
            of(PRIORITY_CHANGED, REPORTER, IN_APP),
            of(INFO_REQUESTED, REPORTER, IN_APP, EMAIL),
            of(SLA_WARNING, ASSIGNEE, IN_APP, PUSH),
            of(SLA_BREACHED, ASSIGNEE, IN_APP, PUSH, EMAIL),
            of(SLA_BREACHED, TEAM_LEAD, IN_APP, PUSH, EMAIL),
            of(ESCALATED, ESCALATION_TARGET, IN_APP, PUSH, EMAIL),
            of(RESOLVED, REPORTER, IN_APP, EMAIL),
            of(RESOLVED, FOLLOWER, IN_APP, EMAIL),
            of(CLOSED, REPORTER, IN_APP),
            of(MAJOR_DECLARED, IT_MANAGER, PUSH, EMAIL),
            of(MAJOR_DECLARED, ADMIN, PUSH, EMAIL),
            of(MAJOR_DECLARED, TEAM_LEAD, PUSH, EMAIL),
            of(MAJOR_DECLARED, AFFECTED_REPORTER, IN_APP),
            of(TEST, ADMIN, IN_APP, EMAIL, PUSH));

    private final List<NotificationRule> rules;

    public RuleTableChannelSelectionPolicy(List<NotificationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    @Override
    public Set<ChannelType> channelsFor(String eventType, RecipientRole role, Priority priority) {
        Set<ChannelType> result = EnumSet.noneOf(ChannelType.class);
        rules.stream().filter(r -> r.matches(eventType, role, priority)).forEach(r -> result.addAll(r.channels()));
        return result;
    }
}
