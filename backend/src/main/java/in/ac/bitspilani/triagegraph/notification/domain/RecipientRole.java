package in.ac.bitspilani.triagegraph.notification.domain;

/** Why someone is being told about an event; the rule table keys on this. */
public enum RecipientRole {
    REPORTER, FOLLOWER, ASSIGNEE, TEAM_LEAD, ESCALATION_TARGET, IT_MANAGER, ADMIN, AFFECTED_REPORTER
}
