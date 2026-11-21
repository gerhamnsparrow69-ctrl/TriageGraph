package in.ac.bitspilani.triagegraph.assignment.application;

import java.time.Clock;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentSettings;
import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentSettingsRepository;
import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentStrategy;
import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentStrategyRegistry;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;

/** Reads and changes the active assignment strategy. The first read creates the settings row. */
@Service
public class AssignmentSettingsService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentSettingsService.class);

    private final AssignmentSettingsRepository repository;
    private final AssignmentStrategyRegistry registry;
    private final AssignmentProperties properties;
    private final Clock clock;

    public AssignmentSettingsService(AssignmentSettingsRepository repository, AssignmentStrategyRegistry registry,
            AssignmentProperties properties, Clock clock) {
        this.repository = repository;
        this.registry = registry;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public AssignmentSettings current() {
        return repository.find().orElseGet(() -> {
            String key = registry.contains(properties.defaultStrategy())
                    ? registry.get(properties.defaultStrategy()).key() : registry.firstKey();
            return repository.save(AssignmentSettings.initial(key, Instant.now(clock)));
        });
    }

    @Transactional
    public AssignmentStrategy activeStrategy() {
        return registry.get(current().getActiveStrategy());
    }

    /** @throws InvalidInputException unknown key (it comes from the admin's request) */
    @Transactional
    public AssignmentSettings switchTo(String strategyKey, Long actorId) {
        AssignmentStrategy strategy = requireKnown(registry, strategyKey);
        AssignmentSettings settings = current();
        String previous = settings.getActiveStrategy();
        settings.switchTo(strategy.key(), actorId, Instant.now(clock));
        AssignmentSettings saved = repository.save(settings);
        log.info("Assignment strategy changed from {} to {} by user {}", previous, strategy.key(), actorId);
        return saved;
    }

    /** User-facing lookup: an unknown key is bad input (400), not a server misconfiguration. */
    static AssignmentStrategy requireKnown(AssignmentStrategyRegistry registry, String key) {
        if (!registry.contains(key)) {
            throw new InvalidInputException("Unknown assignment strategy '" + key + "'. Choose one of "
                    + registry.all().stream().map(AssignmentStrategy::key).toList() + ".");
        }
        return registry.get(key);
    }
}
