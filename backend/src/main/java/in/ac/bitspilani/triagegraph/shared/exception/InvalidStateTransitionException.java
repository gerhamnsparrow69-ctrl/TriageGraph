package in.ac.bitspilani.triagegraph.shared.exception;

/** 409. */
public class InvalidStateTransitionException extends BusinessRuleException {

    public static final String CODE = "INVALID_STATE_TRANSITION";

    public InvalidStateTransitionException(String message) {
        super(CODE, message);
    }

    public InvalidStateTransitionException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
