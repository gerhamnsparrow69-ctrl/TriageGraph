package in.ac.bitspilani.triagegraph.triage.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;
import in.ac.bitspilani.triagegraph.shared.exception.IncidentNotFoundException;
import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeDocument;
import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeRepository;
import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeView;
import in.ac.bitspilani.triagegraph.triage.domain.ServiceCatalog;
import in.ac.bitspilani.triagegraph.triage.domain.TextEmbedder;
import in.ac.bitspilani.triagegraph.triage.domain.TextNormalizer;
import in.ac.bitspilani.triagegraph.triage.domain.VectorIndex;

/**
 * Keeps the knowledge base (database table + in-memory vector index) in step with incidents. The database row
 * is written first; the index is updated after the transaction commits.
 */
@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    /** A resolved incident from the archive (seed data). */
    public record ArchivedIncident(String code, String title, String description, String categoryCode,
            String serviceName, String resolution, String rootCause, Long resolverId, Instant createdAt,
            Instant resolvedAt) {
    }

    /** A document's text and vector, for similarity queries about one incident. */
    public record Indexed(KnowledgeView view, String text, float[] vector) {
    }

    private final KnowledgeRepository repository;
    private final VectorIndex index;
    private final TextEmbedder embedder;
    private final ObjectProvider<IncidentQueryPort> incidents;
    private final ServiceCatalog catalog;
    private final Clock clock;

    public KnowledgeBaseService(KnowledgeRepository repository, VectorIndex index, TextEmbedder embedder,
            ObjectProvider<IncidentQueryPort> incidents, ServiceCatalog catalog, Clock clock) {
        this.repository = repository;
        this.index = index;
        this.embedder = embedder;
        this.incidents = incidents;
        this.catalog = catalog;
        this.clock = clock;
    }

    /** Adds a seeded historical incident. Returns false if its code is already stored (seeding is idempotent). */
    @Transactional
    public boolean addArchived(ArchivedIncident a) {
        if (repository.existsByCode(a.code())) {
            return false;
        }
        Optional<ServiceCatalog.ServiceInfo> service = catalog.byName(a.serviceName());
        KnowledgeDocument doc = KnowledgeDocument.archived(a.code(), a.title(), a.description(), a.categoryCode(),
                service.map(ServiceCatalog.ServiceInfo::id).orElse(null),
                service.map(ServiceCatalog.ServiceInfo::name).orElse(a.serviceName()),
                a.resolution(), a.rootCause(), a.resolverId(), a.createdAt(), a.resolvedAt());
        KnowledgeDocument saved = repository.save(doc);
        putAfterCommit(saved);
        return true;
    }

    /**
     * Copies a live incident into the knowledge base (called by Member 1's lifecycle through
     * {@code TriageService.index}, and again by the event listener as a safety net). Structured fields come
     * from the incident snapshot when Member 1's module is deployed.
     */
    @Transactional
    public KnowledgeView indexLive(long incidentId, String text) {
        Optional<IncidentSnapshot> snapshot = snapshot(incidentId);
        String title = snapshot.map(IncidentSnapshot::title).orElseGet(() -> firstSentence(text));
        String description = snapshot.map(IncidentSnapshot::description).orElse(TextNormalizer.clean(text));
        Optional<KnowledgeDocument> existing = repository.findByIncidentId(incidentId);
        KnowledgeDocument doc = existing.orElseGet(() -> KnowledgeDocument.live(incidentId,
                snapshot.map(IncidentSnapshot::code).orElse("INC-" + incidentId), title, description,
                snapshot.map(IncidentSnapshot::status).orElse(IncidentStatus.NEW),
                snapshot.map(IncidentSnapshot::createdAt).orElse(Instant.now(clock)),
                snapshot.map(IncidentSnapshot::reporterId).orElse(null)));
        if (existing.isPresent()) {
            doc.updateText(title, description);
            snapshot.ifPresent(s -> {
                doc.changeStatus(s.status());
                doc.attachReporter(s.reporterId());
            });
        }
        snapshot.ifPresent(s -> doc.classify(s.categoryCode(), s.serviceId(), serviceName(s.serviceId())));
        KnowledgeDocument saved = repository.save(doc);
        putAfterCommit(saved);
        return saved.view();
    }

    /** Makes sure an incident is in the knowledge base (no-op if it already is). */
    @Transactional
    public void ensureIndexed(long incidentId) {
        if (repository.findByIncidentId(incidentId).isEmpty()) {
            snapshot(incidentId).ifPresent(s -> indexLive(incidentId, TextNormalizer.combine(s.title(), s.description())));
        }
    }

    @Transactional
    public void applyClassification(long incidentId, String categoryCode, Long serviceId) {
        repository.findByIncidentId(incidentId).ifPresent(doc -> {
            doc.classify(categoryCode, serviceId, serviceId == null ? null : serviceName(serviceId));
            KnowledgeView view = repository.save(doc).view();
            AfterCommit.run(() -> index.updateView(view));
        });
    }

    /**
     * Status changes keep duplicate detection to open incidents only. When an incident is resolved, the reason
     * text (the resolution summary, by convention with Member 1) becomes reusable knowledge.
     */
    @Transactional
    public void applyStatus(long incidentId, IncidentStatus status, String reason, Instant at) {
        ensureIndexed(incidentId);
        repository.findByIncidentId(incidentId).ifPresent(doc -> {
            doc.changeStatus(status);
            if (status == IncidentStatus.RESOLVED) {
                doc.recordResolution(resolutionText(reason), snapshot(incidentId).map(IncidentSnapshot::assigneeId).orElse(null), at);
            }
            KnowledgeView view = repository.save(doc).view();
            AfterCommit.run(() -> index.updateView(view));
        });
    }

    /** The incident module sends "Resolved: <summary>" as the status-change reason; keep only the summary. */
    static String resolutionText(String reason) {
        if (reason == null) {
            return null;
        }
        String r = reason.strip();
        return r.regionMatches(true, 0, "Resolved:", 0, 9) ? r.substring(9).strip() : r;
    }

    /** The incident's text and vector; indexes it on the fly if needed. */
    @Transactional
    public Indexed indexed(long incidentId) {
        ensureIndexed(incidentId);
        KnowledgeDocument doc = repository.findByIncidentId(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException("No incident with id " + incidentId + " in triage."));
        String text = doc.text();
        return new Indexed(doc.view(), text, embedder.embed(text));
    }

    /** Fills the in-memory index from the database after startup (and after seeding). */
    @EventListener(ApplicationReadyEvent.class)
    public void rebuildIndex() {
        int added = 0;
        List<KnowledgeDocument> all = repository.findAll();
        for (KnowledgeDocument doc : all) {
            if (!index.contains(doc.getId())) {
                index.put(doc.view(), embedder.embed(doc.text()));
                added++;
            }
        }
        log.info("Triage index ready: {} entries ({} embedded now) using {}", index.size(), added, embedder.name());
    }

    private void putAfterCommit(KnowledgeDocument saved) {
        KnowledgeView view = saved.view();
        float[] vector = embedder.embed(saved.text());
        AfterCommit.run(() -> index.put(view, vector));
    }

    private Optional<IncidentSnapshot> snapshot(long incidentId) {
        IncidentQueryPort port = incidents.getIfAvailable();
        return port == null ? Optional.empty() : port.find(incidentId);
    }

    private String serviceName(Long serviceId) {
        return serviceId == null ? null : catalog.byId(serviceId).map(ServiceCatalog.ServiceInfo::name).orElse(null);
    }

    static String firstSentence(String text) {
        String t = TextNormalizer.clean(text);
        int cut = t.indexOf(". ");
        String first = cut > 0 ? t.substring(0, cut) : t;
        return first.length() <= 120 ? first : first.substring(0, 120);
    }
}
