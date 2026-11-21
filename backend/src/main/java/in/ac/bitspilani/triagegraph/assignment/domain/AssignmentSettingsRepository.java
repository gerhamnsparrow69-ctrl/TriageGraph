package in.ac.bitspilani.triagegraph.assignment.domain;

import java.util.Optional;

public interface AssignmentSettingsRepository {

    Optional<AssignmentSettings> find();

    AssignmentSettings save(AssignmentSettings settings);
}
