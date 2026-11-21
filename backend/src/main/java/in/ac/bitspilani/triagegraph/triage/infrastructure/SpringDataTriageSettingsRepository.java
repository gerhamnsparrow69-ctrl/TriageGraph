package in.ac.bitspilani.triagegraph.triage.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.bitspilani.triagegraph.triage.domain.TriageSettings;

interface SpringDataTriageSettingsRepository extends JpaRepository<TriageSettings, Long> {
}
