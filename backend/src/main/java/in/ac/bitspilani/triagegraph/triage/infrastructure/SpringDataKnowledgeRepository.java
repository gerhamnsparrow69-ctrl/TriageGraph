package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeDocument;

interface SpringDataKnowledgeRepository extends JpaRepository<KnowledgeDocument, Long> {

    Optional<KnowledgeDocument> findByIncidentId(Long incidentId);

    boolean existsByCode(String code);
}
