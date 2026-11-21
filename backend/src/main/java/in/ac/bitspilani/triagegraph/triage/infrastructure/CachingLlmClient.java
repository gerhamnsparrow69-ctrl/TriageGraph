package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import in.ac.bitspilani.triagegraph.triage.domain.LlmClient;
import in.ac.bitspilani.triagegraph.triage.domain.LlmPrompt;

/**
 * Decorator: remembers answers by SHA-256 of the prompt in a small LRU map, so repeating a demo or a
 * debounced preview costs no free-tier quota. Only successful, non-empty answers are cached.
 */
public class CachingLlmClient implements LlmClient {

    private final LlmClient delegate;
    private final Map<String, Object> cache;
    private final AtomicLong hits = new AtomicLong();
    private final AtomicLong misses = new AtomicLong();

    public CachingLlmClient(LlmClient delegate, int maxEntries) {
        this.delegate = delegate;
        int max = Math.max(1, maxEntries);
        this.cache = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
                return size() > max;
            }
        };
    }

    @Override
    public Optional<String> generateText(LlmPrompt prompt) {
        String key = key("text", prompt, null);
        Object cached = get(key);
        if (cached instanceof String s) {
            return Optional.of(s);
        }
        Optional<String> answer = delegate.generateText(prompt);
        answer.ifPresent(a -> put(key, a));
        return answer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> generateJson(LlmPrompt prompt, Map<String, Object> responseSchema) {
        String key = key("json", prompt, responseSchema);
        Object cached = get(key);
        if (cached instanceof Map<?, ?> m) {
            return Optional.of((Map<String, Object>) m);
        }
        Optional<Map<String, Object>> answer = delegate.generateJson(prompt, responseSchema);
        answer.ifPresent(a -> put(key, Map.copyOf(a)));
        return answer;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public boolean enabled() {
        return delegate.enabled();
    }

    @Override
    public String status() {
        return delegate.status() + "; cache " + hits.get() + " hits / " + misses.get() + " misses";
    }

    public long hits() {
        return hits.get();
    }

    private Object get(String key) {
        Object v;
        synchronized (cache) {
            v = cache.get(key);
        }
        (v == null ? misses : hits).incrementAndGet();
        return v;
    }

    private void put(String key, Object value) {
        synchronized (cache) {
            cache.put(key, value);
        }
    }

    static String key(String kind, LlmPrompt p, Map<String, Object> schema) {
        String raw = kind + '\u0000' + p.system() + '\u0000' + p.user() + '\u0000' + p.temperature() + '\u0000'
                + p.maxOutputTokens() + '\u0000' + (schema == null ? "" : Json.write(schema));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available", e);
        }
    }
}
