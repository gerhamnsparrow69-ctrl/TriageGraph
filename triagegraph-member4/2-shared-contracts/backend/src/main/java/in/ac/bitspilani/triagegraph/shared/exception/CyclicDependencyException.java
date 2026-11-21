package in.ac.bitspilani.triagegraph.shared.exception;

/** 422; the graph is left unchanged. */
public class CyclicDependencyException extends BusinessRuleException {

    public static final String CODE = "CYCLIC_DEPENDENCY";

    public CyclicDependencyException(String message) {
        super(CODE, message);
    }

    public CyclicDependencyException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
