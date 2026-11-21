package in.ac.bitspilani.triagegraph.triage.application;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import in.ac.bitspilani.triagegraph.triage.domain.KeywordRepository;
import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeDocument;
import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeRepository;
import in.ac.bitspilani.triagegraph.triage.domain.TriageKeyword;
import in.ac.bitspilani.triagegraph.triage.domain.TriageSettings;
import in.ac.bitspilani.triagegraph.triage.domain.TriageSettingsRepository;

/** In-memory repositories that assign ids the way the database would. */
final class InMemoryRepos {

    private InMemoryRepos() {
    }

    static void assignId(Object entity, long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            if (f.get(entity) == null) {
                f.set(entity, id);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static final class Knowledge implements KnowledgeRepository {
        final List<KnowledgeDocument> docs = new ArrayList<>();
        private final AtomicLong ids = new AtomicLong();

        @Override
        public KnowledgeDocument save(KnowledgeDocument d) {
            assignId(d, ids.incrementAndGet());
            if (!docs.contains(d)) {
                docs.add(d);
            }
            return d;
        }

        @Override
        public Optional<KnowledgeDocument> findByIncidentId(long incidentId) {
            return docs.stream().filter(d -> d.getIncidentId() != null && d.getIncidentId() == incidentId).findFirst();
        }

        @Override
        public boolean existsByCode(String code) {
            return docs.stream().anyMatch(d -> d.getCode().equals(code));
        }

        @Override
        public List<KnowledgeDocument> findAll() {
            return List.copyOf(docs);
        }
    }

    static final class Keywords implements KeywordRepository {
        final List<TriageKeyword> rows = new ArrayList<>();
        private final AtomicLong ids = new AtomicLong();

        @Override
        public List<TriageKeyword> findAll() {
            return List.copyOf(rows);
        }

        @Override
        public TriageKeyword save(TriageKeyword k) {
            assignId(k, ids.incrementAndGet());
            rows.add(k);
            return k;
        }

        @Override
        public void deleteById(long id) {
            rows.removeIf(r -> r.getId() == id);
        }

        @Override
        public boolean existsById(long id) {
            return rows.stream().anyMatch(r -> r.getId() == id);
        }

        @Override
        public long count() {
            return rows.size();
        }
    }

    static final class Settings implements TriageSettingsRepository {
        TriageSettings stored;
        int saves;

        @Override
        public Optional<TriageSettings> find() {
            return Optional.ofNullable(stored);
        }

        @Override
        public TriageSettings save(TriageSettings s) {
            saves++;
            stored = s;
            return s;
        }
    }
}
