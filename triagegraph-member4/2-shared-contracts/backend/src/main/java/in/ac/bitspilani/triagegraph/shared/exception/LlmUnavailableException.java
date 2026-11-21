package in.ac.bitspilani.triagegraph.shared.exception;

public class LlmUnavailableException extends ExternalServiceException {

    public static final String CODE = "LLM_UNAVAILABLE";

    public LlmUnavailableException(String message) {
        super(CODE, message);
    }

    public LlmUnavailableException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
