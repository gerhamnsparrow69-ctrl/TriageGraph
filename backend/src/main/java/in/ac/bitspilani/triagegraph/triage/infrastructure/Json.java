package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A small, dependency-free JSON reader and writer for the Gemini adapter. Keeping the adapter on plain JDK
 * types means it does not depend on which Jackson major version the framework ships (Boot 4 moved to
 * Jackson 3), and it can be tested end-to-end against a local HTTP server.
 * Objects become {@code LinkedHashMap<String, Object>}, arrays {@code List<Object>}, numbers {@code Long} or
 * {@code Double}.
 */
public final class Json {

    private Json() {
    }

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.skipWhitespace();
        Object value = p.value();
        p.skipWhitespace();
        if (!p.atEnd()) {
            throw p.error("unexpected trailing characters");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseObject(String text) {
        Object v = parse(text);
        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("JSON is not an object");
        }
        return (Map<String, Object>) v;
    }

    public static String write(Object value) {
        StringBuilder sb = new StringBuilder();
        write(value, sb);
        return sb.toString();
    }

    private static void write(Object v, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            quote(s, sb);
        } else if (v instanceof Number || v instanceof Boolean) {
            if (v instanceof Double d && (d.isNaN() || d.isInfinite())) {
                sb.append("null");
            } else {
                sb.append(v);
            }
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            Iterator<? extends Map.Entry<?, ?>> it = m.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<?, ?> e = it.next();
                quote(String.valueOf(e.getKey()), sb);
                sb.append(':');
                write(e.getValue(), sb);
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append('}');
        } else if (v instanceof Iterable<?> list) {
            sb.append('[');
            Iterator<?> it = list.iterator();
            while (it.hasNext()) {
                write(it.next(), sb);
                if (it.hasNext()) {
                    sb.append(',');
                }
            }
            sb.append(']');
        } else if (v instanceof Enum<?> e) {
            quote(e.name(), sb);
        } else {
            quote(v.toString(), sb);
        }
    }

    private static void quote(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    private static final class Parser {
        private final String s;
        private int i;

        Parser(String s) {
            this.s = s == null ? "" : s;
        }

        boolean atEnd() {
            return i >= s.length();
        }

        IllegalArgumentException error(String msg) {
            return new IllegalArgumentException("Invalid JSON at position " + i + ": " + msg);
        }

        void skipWhitespace() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        Object value() {
            if (atEnd()) {
                throw error("unexpected end");
            }
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> object();
                case '[' -> array();
                case '"' -> string();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> {
                    if (c == '-' || Character.isDigit(c)) {
                        yield number();
                    }
                    throw error("unexpected character '" + c + "'");
                }
            };
        }

        private Map<String, Object> object() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++; // {
            skipWhitespace();
            if (peek('}')) {
                i++;
                return m;
            }
            while (true) {
                skipWhitespace();
                if (!peek('"')) {
                    throw error("expected a key");
                }
                String key = string();
                skipWhitespace();
                expect(':');
                skipWhitespace();
                m.put(key, value());
                skipWhitespace();
                if (peek(',')) {
                    i++;
                } else {
                    expect('}');
                    return m;
                }
            }
        }

        private List<Object> array() {
            List<Object> list = new ArrayList<>();
            i++; // [
            skipWhitespace();
            if (peek(']')) {
                i++;
                return list;
            }
            while (true) {
                skipWhitespace();
                list.add(value());
                skipWhitespace();
                if (peek(',')) {
                    i++;
                } else {
                    expect(']');
                    return list;
                }
            }
        }

        private String string() {
            i++; // opening quote
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (atEnd()) {
                    throw error("unterminated string");
                }
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c != '\\') {
                    sb.append(c);
                    continue;
                }
                if (atEnd()) {
                    throw error("bad escape");
                }
                char e = s.charAt(i++);
                switch (e) {
                    case '"', '\\', '/' -> sb.append(e);
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        if (i + 4 > s.length()) {
                            throw error("bad unicode escape");
                        }
                        sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw error("bad escape \\" + e);
                }
            }
        }

        private Object number() {
            int start = i;
            if (peek('-')) {
                i++;
            }
            boolean fraction = false;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (Character.isDigit(c)) {
                    i++;
                } else if (c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') {
                    fraction = true;
                    i++;
                } else {
                    break;
                }
            }
            String n = s.substring(start, i);
            try {
                return fraction ? (Object) Double.parseDouble(n) : (Object) Long.parseLong(n);
            } catch (NumberFormatException ex) {
                throw error("bad number " + n);
            }
        }

        private Object literal(String word, Object value) {
            if (!s.startsWith(word, i)) {
                throw error("expected " + word);
            }
            i += word.length();
            return value;
        }

        private boolean peek(char c) {
            return i < s.length() && s.charAt(i) == c;
        }

        private void expect(char c) {
            if (!peek(c)) {
                throw error("expected '" + c + "'");
            }
            i++;
        }
    }
}
