package in.ac.bitspilani.triagegraph.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.notification.domain.AbstractNotificationChannel;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.NotificationMessage;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.exception.NotificationDeliveryException;

class RetryingNotificationChannelTest {

    private static final UserRef SNEHA = new UserRef(10, "Sneha Reddy", Role.TECHNICIAN, "sneha@campus.test", "tg-sneha");
    private static final NotificationMessage MSG = new NotificationMessage(10, "s", "b", null, "TEST", null);

    private final List<Duration> waits = new ArrayList<>();

    /** Fails the first {@code failures} calls, then succeeds. */
    static class Flaky extends AbstractNotificationChannel {
        int calls;
        final int failures;

        Flaky(int failures) {
            this.failures = failures;
        }

        @Override
        public ChannelType type() {
            return ChannelType.EMAIL;
        }

        @Override
        protected void deliver(UserRef r, NotificationMessage m) {
            calls++;
            if (calls <= failures) {
                throw new IllegalStateException("SMTP down");
            }
        }
    }

    @Test
    void succeedsOnALaterAttempt() {
        Flaky flaky = new Flaky(2);
        new RetryingNotificationChannel(flaky, 3, Duration.ofMillis(100), waits::add).send(SNEHA, MSG);
        assertThat(flaky.calls).isEqualTo(3);
        assertThat(waits).containsExactly(Duration.ofMillis(100), Duration.ofMillis(200));
    }

    /** TC20 at unit level: three attempts, then the failure surfaces as NotificationDeliveryException. */
    @Test
    void givesUpAfterThreeAttempts() {
        Flaky flaky = new Flaky(99);
        var retrying = new RetryingNotificationChannel(flaky, 3, Duration.ofMillis(100), waits::add);
        assertThatThrownBy(() -> retrying.send(SNEHA, MSG))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessageContaining("SMTP down");
        assertThat(flaky.calls).isEqualTo(3);
        assertThat(waits).hasSize(2);
    }

    @Test
    void keepsTheWrappedChannelsIdentity() {
        var retrying = new RetryingNotificationChannel(new Flaky(0), 3, Duration.ZERO, waits::add);
        assertThat(retrying.type()).isEqualTo(ChannelType.EMAIL);
        assertThat(retrying.canReach(SNEHA)).isTrue();
        assertThatThrownBy(() -> new RetryingNotificationChannel(new Flaky(0), 0, Duration.ZERO, waits::add))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
