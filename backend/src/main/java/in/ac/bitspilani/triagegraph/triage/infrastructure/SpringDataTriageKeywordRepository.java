package in.ac.bitspilani.triagegraph.triage.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.bitspilani.triagegraph.triage.domain.TriageKeyword;

interface SpringDataTriageKeywordRepository extends JpaRepository<TriageKeyword, Long> {
}
