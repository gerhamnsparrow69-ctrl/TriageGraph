package in.ac.bitspilani.triagegraph.notification.infrastructure;

/** Adapter boundary for the ntfy push service, so tests can swap in a fake. */
public interface NtfyGateway {

    /** @param priority ntfy priority 1 (min) to 5 (max, makes the phone buzz) */
    void publish(String topic, String title, String message, int priority);
}
