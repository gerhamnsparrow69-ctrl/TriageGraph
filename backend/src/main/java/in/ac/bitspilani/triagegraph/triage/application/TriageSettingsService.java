package in.ac.bitspilani.triagegraph.triage.application;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.triage.domain.DuplicatePolicy;
import in.ac.bitspilani.triagegraph.triage.domain.TriageSettings;
import in.ac.bitspilani.triagegraph.triage.domain.TriageSettingsRepository;

/** Admin-editable thresholds and the AI switch. Cached in memory because every preview reads them. */
@Service
public class TriageSettingsService {

    private final TriageSettingsRepository repository;
    private final TriageProperties properties;
    private final Clock clock;
    private volatile TriageSettings cached;

    public TriageSettingsService(TriageSettingsRepository repository, TriageProperties properties, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public TriageSettings current() {
        TriageSettings s = cached;
        if (s == null) {
            s = repository.find().orElseGet(() -> repository.save(TriageSettings.initial(
                    properties.likelyDuplicateThreshold(), properties.possiblyRelatedThreshold(),
                    (int) Math.max(1, properties.duplicateWindow().toHours()), properties.llm().enabled(),
                    Instant.now(clock))));
            cached = s;
        }
        return s;
    }

    /** @throws in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException invalid values */
    @Transactional
    public TriageSettings update(double likely, double possibly, int windowHours, boolean aiEnabled, Long actorId) {
        TriageSettings s = repository.find().orElseGet(this::current);
        s.update(likely, possibly, windowHours, aiEnabled, actorId, Instant.now(clock));
        TriageSettings saved = repository.save(s);
        cached = saved;
        return saved;
    }

    public DuplicatePolicy policy() {
        return current().policy();
    }

    /** The admin switch AND a configured key: both are needed before anything is sent to Gemini. */
    public boolean aiEnabled() {
        return current().isAiEnabled();
    }
}
