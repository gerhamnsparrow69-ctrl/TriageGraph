package in.ac.bitspilani.triagegraph.shared.exception;

/** Maps to HTTP 404. */
public abstract class ResourceNotFoundException extends ItsmException {

    protected ResourceNotFoundException(String errorCode, String message) {
        super(errorCode, message);
    }

    protected ResourceNotFoundException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
