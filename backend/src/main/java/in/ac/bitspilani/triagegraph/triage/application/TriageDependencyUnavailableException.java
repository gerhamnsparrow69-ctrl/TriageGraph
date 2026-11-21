package in.ac.bitspilani.triagegraph.triage.application;

import in.ac.bitspilani.triagegraph.shared.exception.ExternalServiceException;

/** A module this feature needs (usually Member 1's incident module) is not deployed yet. HTTP 503. */
public class TriageDependencyUnavailableException extends ExternalServiceException {

    public static final String CODE = "MODULE_UNAVAILABLE";

    public TriageDependencyUnavailableException(String message) {
        super(CODE, message);
    }
}
