package in.ac.bitspilani.triagegraph.shared.exception;

/** Invalid login. Deliberately never says which field was wrong. */
public class AuthenticationFailedException extends ItsmException {

    public static final String CODE = "AUTH_FAILED";

    public AuthenticationFailedException(String message) {
        super(CODE, message);
    }

    public AuthenticationFailedException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
