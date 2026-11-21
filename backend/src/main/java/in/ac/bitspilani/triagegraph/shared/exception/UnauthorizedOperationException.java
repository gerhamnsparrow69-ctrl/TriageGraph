package in.ac.bitspilani.triagegraph.shared.exception;

/** The user may not perform this operation (403). */
public class UnauthorizedOperationException extends ItsmException {

    public static final String CODE = "FORBIDDEN";

    public UnauthorizedOperationException(String message) {
        super(CODE, message);
    }

    public UnauthorizedOperationException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
