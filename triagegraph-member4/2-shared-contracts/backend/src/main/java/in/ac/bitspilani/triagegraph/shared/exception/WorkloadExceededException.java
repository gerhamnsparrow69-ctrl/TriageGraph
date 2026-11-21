package in.ac.bitspilani.triagegraph.shared.exception;

/** Everyone with the skill is at max load. */
public class WorkloadExceededException extends AssignmentException {

    public static final String CODE = "WORKLOAD_EXCEEDED";

    public WorkloadExceededException(String message) {
        super(CODE, message);
    }

    public WorkloadExceededException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
