package in.ac.bitspilani.triagegraph.shared.exception;

public class SlaConfigurationMissingException extends ConfigurationException {

    public static final String CODE = "SLA_CONFIG_MISSING";

    public SlaConfigurationMissingException(String message) {
        super(CODE, message);
    }

    public SlaConfigurationMissingException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
