package in.ac.bitspilani.triagegraph.shared.exception;

/** Maps to HTTP 409 / 422. */
public abstract class BusinessRuleException extends ItsmException {

    protected BusinessRuleException(String errorCode, String message) {
        super(errorCode, message);
    }

    protected BusinessRuleException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
