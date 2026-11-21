package in.ac.bitspilani.triagegraph.shared.exception;

public class StrategyNotFoundException extends ConfigurationException {

    public static final String CODE = "STRATEGY_NOT_FOUND";

    public StrategyNotFoundException(String message) {
        super(CODE, message);
    }

    public StrategyNotFoundException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
