package in.ac.bitspilani.triagegraph.shared.exception;

/** 409, with a link to the existing ticket in the message. */
public class DuplicateIncidentException extends BusinessRuleException {

    public static final String CODE = "DUPLICATE_INCIDENT";

    public DuplicateIncidentException(String message) {
        super(CODE, message);
    }

    public DuplicateIncidentException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
