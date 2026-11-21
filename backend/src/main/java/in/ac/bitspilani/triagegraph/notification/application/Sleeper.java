package in.ac.bitspilani.triagegraph.notification.application;

import java.time.Duration;

/** Lets tests retry without actually waiting. */
@FunctionalInterface
public interface Sleeper {

    void sleep(Duration d);

    Sleeper REAL = d -> {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    };
}
