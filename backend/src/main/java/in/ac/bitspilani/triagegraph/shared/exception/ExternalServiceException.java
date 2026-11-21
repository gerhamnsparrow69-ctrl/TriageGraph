package in.ac.bitspilani.triagegraph.shared.exception;

/** Maps to HTTP 503; never breaks the incident flow. */
public abstract class ExternalServiceException extends ItsmException {

    protected ExternalServiceException(String errorCode, String message) {
        super(errorCode, message);
    }

    protected ExternalServiceException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
