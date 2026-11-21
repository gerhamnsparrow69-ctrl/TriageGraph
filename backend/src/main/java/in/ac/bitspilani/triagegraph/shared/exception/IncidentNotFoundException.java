package in.ac.bitspilani.triagegraph.shared.exception;

public class IncidentNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "INCIDENT_NOT_FOUND";

    public IncidentNotFoundException(String message) {
        super(CODE, message);
    }

    public IncidentNotFoundException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
