package in.ac.bitspilani.triagegraph.shared.exception;

public class UserNotFoundException extends ResourceNotFoundException {

    public static final String CODE = "USER_NOT_FOUND";

    public UserNotFoundException(String message) {
        super(CODE, message);
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
