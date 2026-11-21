package in.ac.bitspilani.triagegraph.assignment.application;

import in.ac.bitspilani.triagegraph.shared.exception.ExternalServiceException;

/**
 * A module this feature needs (for example Member 1's incident module) is not deployed yet.
 * Maps to HTTP 503 through the shared handler, like any {@link ExternalServiceException}.
 */
public class AssignmentDependencyUnavailableException extends ExternalServiceException {

    public static final String CODE = "MODULE_UNAVAILABLE";

    public AssignmentDependencyUnavailableException(String message) {
        super(CODE, message);
    }
}
