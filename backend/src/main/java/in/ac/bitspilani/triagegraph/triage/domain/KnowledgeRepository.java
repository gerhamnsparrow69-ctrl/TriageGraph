package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.List;
import java.util.Optional;

public interface KnowledgeRepository {

    KnowledgeDocument save(KnowledgeDocument document);

    Optional<KnowledgeDocument> findByIncidentId(long incidentId);

    boolean existsByCode(String code);

    List<KnowledgeDocument> findAll();
}
