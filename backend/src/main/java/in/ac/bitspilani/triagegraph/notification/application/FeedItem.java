package in.ac.bitspilani.triagegraph.notification.application;

import java.time.Instant;

/** One line in the Command Centre's live event feed. */
public record FeedItem(String type, String incidentCode, String summary, Instant at) {
}
