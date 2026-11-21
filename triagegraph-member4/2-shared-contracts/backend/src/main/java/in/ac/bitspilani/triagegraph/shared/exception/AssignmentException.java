package in.ac.bitspilani.triagegraph.shared.exception;

/** Maps to HTTP 409. */
public abstract class AssignmentException extends ItsmException {

    protected AssignmentException(String errorCode, String message) {
        super(errorCode, message);
    }

    protected AssignmentException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
