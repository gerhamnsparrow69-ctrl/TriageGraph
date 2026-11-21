package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeDocument;
import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeRepository;

@Repository
class JpaKnowledgeRepository implements KnowledgeRepository {

    private final SpringDataKnowledgeRepository jpa;

    JpaKnowledgeRepository(SpringDataKnowledgeRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        return jpa.save(document);
    }

    @Override
    public Optional<KnowledgeDocument> findByIncidentId(long incidentId) {
        return jpa.findByIncidentId(incidentId);
    }

    @Override
    public boolean existsByCode(String code) {
        return jpa.existsByCode(code);
    }

    @Override
    public List<KnowledgeDocument> findAll() {
        return jpa.findAll();
    }
}
