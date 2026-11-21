package in.ac.bitspilani.triagegraph.shared.contracts.enums;

/** Everything a user can do to an incident. Responses list the allowed ones so the UI never hard-codes rules. */
public enum IncidentAction {
    CLASSIFY, ASSIGN, ACKNOWLEDGE, START_WORK, REQUEST_INFO, PROVIDE_INFO,
    RESOLVE, VERIFY, REOPEN, CLOSE, MERGE, ADD_NOTE, FEEDBACK, ESCALATE
}
