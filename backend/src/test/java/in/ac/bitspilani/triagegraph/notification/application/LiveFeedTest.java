package in.ac.bitspilani.triagegraph.notification.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

class LiveFeedTest {

    private static final Instant T0 = Instant.parse("2026-11-21T10:00:00Z");

    private final List<Object> pushed = new ArrayList<>();
    private final LiveFeed feed = new LiveFeed(new LivePush() {
        @Override
        public void toUser(long userId, String event, Object data) {
        }

        @Override
        public void toRoles(Set<Role> roles, String event, Object data) {
            pushed.add(data);
        }
    });

    @Test
    void publishPushesToWatchersAndKeepsNewestFirst() {
        FeedItem a = new FeedItem("REPORTED", "INC-1", "first", T0);
        FeedItem b = new FeedItem("ASSIGNED", "INC-1", "second", T0.plusSeconds(5));

        feed.publish(a);
        feed.publish(b);

        assertThat(pushed).containsExactly(a, b);
        assertThat(feed.recent()).containsExactly(b, a);
    }

    @Test
    void historyIsCapped() {
        for (int i = 0; i < LiveFeed.HISTORY + 10; i++) {
            feed.publish(new FeedItem("TEST", null, "item " + i, T0.plusSeconds(i)));
        }

        assertThat(feed.recent()).hasSize(LiveFeed.HISTORY);
        assertThat(feed.recent().getFirst().summary()).isEqualTo("item " + (LiveFeed.HISTORY + 9));
    }
}
