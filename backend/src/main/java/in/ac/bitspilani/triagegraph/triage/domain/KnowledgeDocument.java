package in.ac.bitspilani.triagegraph.triage.domain;

import java.time.Instant;
import java.util.Objects;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * One entry in the triage knowledge base: either a seeded historical incident with its resolution, or a copy
 * of a live incident's text and status. Triage owns this table, so it never needs another module's tables
 * and keeps working while Member 1's incident module is still being built.
 */
@Entity
@Table(name = "triage_document")
public class KnowledgeDocument {

    public static final int MAX_TITLE = 200;
    public static final int MAX_DESCRIPTION = 4000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    @Column(name = "incident_id", unique = true)
    private Long incidentId;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(nullable = false, length = MAX_TITLE)
    private String title;

    @Column(length = MAX_DESCRIPTION)
    private String description;

    @Column(name = "category_code", length = 40)
    private String categoryCode;

    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private KnowledgeSource source;

    @Column(name = "reporter_id")
    private Long reporterId;

    @Column(name = "resolution_summary", length = 2000)
    private String resolutionSummary;

    @Column(name = "root_cause", length = 500)
    private String rootCause;

    @Column(name = "resolver_id")
    private Long resolverId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected KnowledgeDocument() {
    }

    private KnowledgeDocument(KnowledgeSource source, String code, String title, String description,
            IncidentStatus status, Instant createdAt) {
        this.source = Objects.requireNonNull(source);
        this.code = Objects.requireNonNull(code);
        updateText(title, description);
        this.status = Objects.requireNonNull(status);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    /** A resolved incident from the archive, with how it was fixed. */
    public static KnowledgeDocument archived(String code, String title, String description, String categoryCode,
            Long serviceId, String serviceName, String resolution, String rootCause, Long resolverId,
            Instant createdAt, Instant resolvedAt) {
        KnowledgeDocument d = new KnowledgeDocument(KnowledgeSource.ARCHIVE, code, title, description,
                IncidentStatus.CLOSED, createdAt);
        d.classify(categoryCode, serviceId, serviceName);
        d.resolutionSummary = truncate(resolution, 2000);
        d.rootCause = truncate(rootCause, 500);
        d.resolverId = resolverId;
        d.resolvedAt = resolvedAt;
        return d;
    }

    /** A copy of a live incident reported in this system. */
    public static KnowledgeDocument live(long incidentId, String code, String title, String description,
            IncidentStatus status, Instant createdAt, Long reporterId) {
        KnowledgeDocument d = new KnowledgeDocument(KnowledgeSource.LIVE, code, title, description, status, createdAt);
        d.incidentId = incidentId;
        d.reporterId = reporterId;
        return d;
    }

    public void updateText(String newTitle, String newDescription) {
        String t = TextNormalizer.clean(newTitle);
        this.title = truncate(t.isEmpty() ? "(no title)" : t, MAX_TITLE);
        this.description = truncate(TextNormalizer.clean(newDescription), MAX_DESCRIPTION);
    }

    public void classify(String newCategory, Long newServiceId, String newServiceName) {
        if (newCategory != null && !newCategory.isBlank()) {
            this.categoryCode = newCategory.trim().toUpperCase(java.util.Locale.ROOT);
        }
        if (newServiceId != null) {
            this.serviceId = newServiceId;
        }
        if (newServiceName != null && !newServiceName.isBlank()) {
            this.serviceName = newServiceName.trim();
        }
    }

    public void changeStatus(IncidentStatus newStatus) {
        this.status = Objects.requireNonNull(newStatus);
    }

    public void attachReporter(Long reporter) {
        if (reporter != null) {
            this.reporterId = reporter;
        }
    }

    /** Stores how the incident was fixed, so similar future incidents can reuse it. */
    public void recordResolution(String summary, Long resolver, Instant at) {
        if (summary != null && !summary.isBlank()) {
            this.resolutionSummary = truncate(summary.trim(), 2000);
        }
        if (resolver != null) {
            this.resolverId = resolver;
        }
        this.resolvedAt = at;
    }

    /** The text that is embedded: "Title. Description". */
    public String text() {
        return TextNormalizer.combine(title, description);
    }

    public KnowledgeView view() {
        return new KnowledgeView(id == null ? 0 : id, incidentId, code, title, status, categoryCode, serviceId,
                serviceName, createdAt, reporterId, resolutionSummary, rootCause, resolverId, source);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    public Long getId() {
        return id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public KnowledgeSource getSource() {
        return source;
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
