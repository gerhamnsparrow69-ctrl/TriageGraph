package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.regex.Pattern;

/**
 * Data minimisation before text leaves the campus (Gemini calls): e-mail addresses, phone numbers and
 * BITS ID numbers are replaced with placeholders. The local MiniLM model sees the original text.
 */
public final class PiiMasker {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    /** BITS ID such as 2024A7PS0123H. */
    private static final Pattern BITS_ID = Pattern.compile("\\b20\\d{2}[A-Z][A-Z0-9][A-Z]{2}\\d{4}[A-Z]\\b", Pattern.CASE_INSENSITIVE);
    /** Indian mobile numbers, with or without +91, spaces or dashes. */
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(?:\\+?91[\\s-]?)?[6-9]\\d{4}[\\s-]?\\d{5}(?!\\d)");

    private PiiMasker() {
    }

    public static String mask(String text) {
        if (text == null) {
            return "";
        }
        String out = EMAIL.matcher(text).replaceAll("[email]");
        out = BITS_ID.matcher(out).replaceAll("[student-id]");
        return PHONE.matcher(out).replaceAll("[phone]");
    }
}
