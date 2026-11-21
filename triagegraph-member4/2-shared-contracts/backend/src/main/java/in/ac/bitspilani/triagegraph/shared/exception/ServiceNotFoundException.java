package in.ac.bitspilani.triagegraph.shared.exception;

public class ServiceNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "SERVICE_NOT_FOUND";

    public ServiceNotFoundException(String message) {
        super(CODE, message);
    }

    public ServiceNotFoundException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
