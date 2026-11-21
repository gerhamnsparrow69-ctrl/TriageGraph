package in.ac.bitspilani.triagegraph.triage.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void parsesNestedObjectsArraysNumbersAndEscapes() {
        Map<String, Object> m = Json.parseObject(
                "{\"a\": [1, 2.5, -3e2, true, null], \"b\": {\"c\": \"line\\n\\\"quoted\\\" \\u00e9\"}, \"d\": false}");
        assertEquals(List.of(1L, 2.5, -300.0, true), ((List<?>) m.get("a")).subList(0, 4));
        assertEquals("line\n\"quoted\" é", ((Map<?, ?>) m.get("b")).get("c"));
        assertEquals(Boolean.FALSE, m.get("d"));
    }

    @Test
    void writeThenParseRoundTrips() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("text", "Tab\there, quote \" and backslash \\");
        m.put("list", List.of(1, "two", Map.of("three", 3.0)));
        m.put("none", null);
        assertEquals(m.toString(), Json.parse(Json.write(m)).toString());
        assertEquals("{\"k\":\"v\\u0001\"}", Json.write(Map.of("k", "v\u0001")));
    }

    @Test
    void invalidJsonIsRejectedWithAPosition() {
        var e = assertThrows(IllegalArgumentException.class, () -> Json.parse("{\"a\": }"));
        assertEquals("Invalid JSON at position 6: unexpected character '}'", e.getMessage());
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{} extra"));
        assertThrows(IllegalArgumentException.class, () -> Json.parseObject("[1]"));
    }
}
