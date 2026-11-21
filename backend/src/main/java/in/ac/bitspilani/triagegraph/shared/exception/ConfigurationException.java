package in.ac.bitspilani.triagegraph.shared.exception;

/** Maps to HTTP 500. */
public abstract class ConfigurationException extends ItsmException {

    protected ConfigurationException(String errorCode, String message) {
        super(errorCode, message);
    }

    protected ConfigurationException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
