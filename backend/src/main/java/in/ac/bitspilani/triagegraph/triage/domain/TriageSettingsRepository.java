package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.Optional;

public interface TriageSettingsRepository {

    Optional<TriageSettings> find();

    TriageSettings save(TriageSettings settings);
}
