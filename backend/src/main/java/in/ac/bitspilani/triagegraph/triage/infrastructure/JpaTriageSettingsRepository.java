package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import in.ac.bitspilani.triagegraph.triage.domain.TriageSettings;
import in.ac.bitspilani.triagegraph.triage.domain.TriageSettingsRepository;

@Repository
class JpaTriageSettingsRepository implements TriageSettingsRepository {

    private final SpringDataTriageSettingsRepository jpa;

    JpaTriageSettingsRepository(SpringDataTriageSettingsRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<TriageSettings> find() {
        return jpa.findById(TriageSettings.SINGLETON_ID);
    }

    @Override
    public TriageSettings save(TriageSettings settings) {
        return jpa.save(settings);
    }
}
