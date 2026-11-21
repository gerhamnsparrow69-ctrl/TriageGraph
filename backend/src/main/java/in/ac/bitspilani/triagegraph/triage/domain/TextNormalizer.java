package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.Locale;

/** Prepares incident text for embedding and for keyword matching. */
public final class TextNormalizer {

    /** Long reports are cut here; the first part carries the meaning and MiniLM reads 256 tokens at most. */
    public static final int MAX_CHARS = 1500;

    private TextNormalizer() {
    }

    /** "Title. Description" with whitespace collapsed and length capped. */
    public static String combine(String title, String description) {
        String t = clean(title);
        String d = clean(description);
        String joined;
        if (t.isEmpty()) {
            joined = d;
        } else if (d.isEmpty()) {
            joined = t;
        } else {
            joined = t.matches(".*[.!?]$") ? t + " " + d : t + ". " + d;
        }
        return joined.length() <= MAX_CHARS ? joined : joined.substring(0, MAX_CHARS);
    }

    public static String clean(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    /** Lower case, letters and digits only, single spaces, padded with a space on both sides for phrase search. */
    public static String forMatching(String s) {
        String lower = s == null ? "" : s.toLowerCase(Locale.ROOT).replace('-', ' ');
        return " " + lower.replaceAll("[^a-z0-9]+", " ").trim() + " ";
    }
}
