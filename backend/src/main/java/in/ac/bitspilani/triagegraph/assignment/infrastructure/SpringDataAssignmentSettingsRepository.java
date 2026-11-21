package in.ac.bitspilani.triagegraph.assignment.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentSettings;

interface SpringDataAssignmentSettingsRepository extends JpaRepository<AssignmentSettings, Long> {
}
