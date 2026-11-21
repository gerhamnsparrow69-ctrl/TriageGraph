package in.ac.bitspilani.triagegraph.shared.exception;

/** 409. */
public class FeedbackNotAllowedException extends BusinessRuleException {

    public static final String CODE = "FEEDBACK_NOT_ALLOWED";

    public FeedbackNotAllowedException(String message) {
        super(CODE, message);
    }

    public FeedbackNotAllowedException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
