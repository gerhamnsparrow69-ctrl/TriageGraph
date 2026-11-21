package in.ac.bitspilani.triagegraph.shared.exception;

/** Nobody has the required skill. */
public class TechnicianUnavailableException extends AssignmentException {

    public static final String CODE = "TECHNICIAN_UNAVAILABLE";

    public TechnicianUnavailableException(String message) {
        super(CODE, message);
    }

    public TechnicianUnavailableException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
