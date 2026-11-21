package in.ac.bitspilani.triagegraph.assignment.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentSettings;
import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentSettingsRepository;

@Repository
class JpaAssignmentSettingsRepository implements AssignmentSettingsRepository {

    private final SpringDataAssignmentSettingsRepository jpa;

    JpaAssignmentSettingsRepository(SpringDataAssignmentSettingsRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<AssignmentSettings> find() {
        return jpa.findById(AssignmentSettings.SINGLETON_ID);
    }

    @Override
    public AssignmentSettings save(AssignmentSettings settings) {
        return jpa.save(settings);
    }
}
