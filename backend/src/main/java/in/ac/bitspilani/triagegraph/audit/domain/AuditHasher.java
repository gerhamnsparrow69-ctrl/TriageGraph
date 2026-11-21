package in.ac.bitspilani.triagegraph.audit.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Objects;

/**
 * hash = SHA-256(prevHash ‖ incidentId ‖ actorId ‖ action ‖ fromState ‖ toState ‖ description ‖ timestamp),
 * as lowercase hex. Fields are joined with the ASCII unit separator (0x1F) so that ("ab","c") and
 * ("a","bc") can never hash the same; null is written as the empty string.
 */
public final class AuditHasher {

    /** prevHash of the very first entry. */
    public static final String GENESIS = "0".repeat(64);

    private static final char SEPARATOR = '\u001F';

    private AuditHasher() {
    }

    public static String hash(String prevHash, Long incidentId, Long actorId, AuditAction action,
            Object fromState, Object toState, String description, Instant timestamp) {
        StringBuilder sb = new StringBuilder();
        append(sb, prevHash);
        append(sb, incidentId);
        append(sb, actorId);
        append(sb, action);
        append(sb, fromState);
        append(sb, toState);
        append(sb, description);
        sb.append(timestamp.toString());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available on the JVM", e);
        }
    }

    /**
     * Databases keep microseconds; hashing nanoseconds that the column then drops would make every
     * entry fail verification. Entries are always stored and hashed at this precision.
     */
    public static Instant normalise(Instant t) {
        return Objects.requireNonNull(t).truncatedTo(ChronoUnit.MICROS);
    }

    private static void append(StringBuilder sb, Object value) {
        sb.append(value == null ? "" : value.toString()).append(SEPARATOR);
    }
}
