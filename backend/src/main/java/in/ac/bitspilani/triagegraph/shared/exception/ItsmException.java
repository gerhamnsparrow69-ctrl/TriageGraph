package in.ac.bitspilani.triagegraph.shared.exception;

/**
 * Root of every TriageGraph exception. Unchecked, and carries a stable {@code errorCode} for the UI.
 * The domain never knows about HTTP: {@code shared.api.GlobalExceptionHandler} maps classes to status codes.
 */
public abstract class ItsmException extends RuntimeException {

    private final String errorCode;

    protected ItsmException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected ItsmException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
