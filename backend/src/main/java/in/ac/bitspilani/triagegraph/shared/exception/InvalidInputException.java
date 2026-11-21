package in.ac.bitspilani.triagegraph.shared.exception;

/** Invalid user input (400), beyond what Bean Validation catches. */
public class InvalidInputException extends ItsmException {

    public static final String CODE = "INVALID_INPUT";

    public InvalidInputException(String message) {
        super(CODE, message);
    }

    public InvalidInputException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
