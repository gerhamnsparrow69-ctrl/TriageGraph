package in.ac.bitspilani.triagegraph.shared.exception;

public class NotificationDeliveryException extends ExternalServiceException {

    public static final String CODE = "NOTIFICATION_FAILED";

    public NotificationDeliveryException(String message) {
        super(CODE, message);
    }

    public NotificationDeliveryException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
