package in.ac.bitspilani.triagegraph.shared.exception;

/** Wraps a DataAccessException (503). */
public class PersistenceFailureException extends ItsmException {

    public static final String CODE = "PERSISTENCE_FAILURE";

    public PersistenceFailureException(String message) {
        super(CODE, message);
    }

    public PersistenceFailureException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
