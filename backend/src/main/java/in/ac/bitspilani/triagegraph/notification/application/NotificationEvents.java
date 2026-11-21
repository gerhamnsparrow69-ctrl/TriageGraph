package in.ac.bitspilani.triagegraph.notification.application;

/** Event type keys used by the rule table, messages and the inbox. */
public final class NotificationEvents {

    public static final String REPORTED = "REPORTED";
    public static final String ASSIGNED = "ASSIGNED";
    public static final String PRIORITY_CHANGED = "PRIORITY_CHANGED";
    public static final String INFO_REQUESTED = "INFO_REQUESTED";
    public static final String SLA_WARNING = "SLA_WARNING";
    public static final String SLA_BREACHED = "SLA_BREACHED";
    public static final String ESCALATED = "ESCALATED";
    public static final String RESOLVED = "RESOLVED";
    public static final String CLOSED = "CLOSED";
    public static final String MAJOR_DECLARED = "MAJOR_DECLARED";
    public static final String TEST = "TEST";

    private NotificationEvents() {
    }
}
