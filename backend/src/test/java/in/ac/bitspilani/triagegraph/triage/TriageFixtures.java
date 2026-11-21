package in.ac.bitspilani.triagegraph.triage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeSource;
import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeView;
import in.ac.bitspilani.triagegraph.triage.domain.LlmClient;
import in.ac.bitspilani.triagegraph.triage.domain.LlmPrompt;
import in.ac.bitspilani.triagegraph.triage.domain.ServiceCatalog;
import in.ac.bitspilani.triagegraph.triage.domain.Vectors;

/** Shared test helpers for the triage module. */
public final class TriageFixtures {

    public static final Instant NOW = Instant.parse("2026-11-21T10:00:00Z");

    private TriageFixtures() {
    }

    /** A 2-D unit vector at the given cosine to (1, 0), so test similarities are exact. */
    public static float[] atCosine(double cosine) {
        return new float[] {(float) cosine, (float) Math.sqrt(Math.max(0, 1 - cosine * cosine))};
    }

    public static float[] query() {
        return new float[] {1f, 0f};
    }

    public static KnowledgeView live(long docId, long incidentId, String code, IncidentStatus status, Long serviceId,
            String serviceName, Instant createdAt, Long reporterId) {
        return new KnowledgeView(docId, incidentId, code, "Incident " + code, status, "APPLICATION", serviceId,
                serviceName, createdAt, reporterId, null, null, null, KnowledgeSource.LIVE);
    }

    public static KnowledgeView archived(long docId, String code, String category, String serviceName, String resolution) {
        return new KnowledgeView(docId, null, code, "Archived " + code, IncidentStatus.CLOSED, category, null,
                serviceName, NOW.minusSeconds(86_400L * 30), null, resolution, "a root cause", 4L, KnowledgeSource.ARCHIVE);
    }

    public static float[] unit(float... v) {
        return Vectors.normalise(v);
    }

    /** The campus catalog without Member 3 (names only), or with ids when given. */
    public static ServiceCatalog catalog(Map<String, Long> ids) {
        List<String> names = List.of("Fee Payment App", "Payment Gateway API", "Campus SSO", "LMS", "ERP Portal",
                "Hostel Wi-Fi", "Email & Collaboration", "Print Services", "Lab Workstations", "Database Cluster");
        return new ServiceCatalog() {
            public List<ServiceInfo> services() {
                return names.stream().map(n -> new ServiceInfo(ids.get(n), n)).toList();
            }

            public List<String> categoryCodes() {
                return List.of("HARDWARE", "SOFTWARE", "NETWORK", "DATABASE", "CLOUD", "SECURITY", "AUTHENTICATION",
                        "APPLICATION", "STORAGE");
            }

            public Optional<ServiceInfo> byName(String name) {
                return services().stream().filter(s -> s.name().equalsIgnoreCase(name == null ? "" : name.trim())).findFirst();
            }

            public Optional<ServiceInfo> byId(long id) {
                return services().stream().filter(s -> s.id() != null && s.id() == id).findFirst();
            }
        };
    }

    /** A scripted LLM that records prompts and returns queued answers (or throws). */
    public static final class FakeLlm implements LlmClient {
        public final List<LlmPrompt> prompts = new ArrayList<>();
        public Map<String, Object> json;
        public String text;
        public RuntimeException failure;
        public boolean enabled = true;

        @Override
        public Optional<String> generateText(LlmPrompt prompt) {
            prompts.add(prompt);
            if (failure != null) {
                throw failure;
            }
            return Optional.ofNullable(text);
        }

        @Override
        public Optional<Map<String, Object>> generateJson(LlmPrompt prompt, Map<String, Object> schema) {
            prompts.add(prompt);
            if (failure != null) {
                throw failure;
            }
            return Optional.ofNullable(json);
        }

        @Override
        public String name() {
            return "fake";
        }

        @Override
        public boolean enabled() {
            return enabled;
        }
    }

    public static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                if (value == null) {
                    throw new IllegalStateException("not available");
                }
                return value;
            }

            @Override
            public T getObject(Object... args) {
                return getObject();
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }
        };
    }
}
