package in.ac.bitspilani.triagegraph.triage.application;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.triage.domain.KeywordRepository;
import in.ac.bitspilani.triagegraph.triage.domain.KeywordRule;
import in.ac.bitspilani.triagegraph.triage.domain.KeywordRules;
import in.ac.bitspilani.triagegraph.triage.domain.ServiceCatalog;
import in.ac.bitspilani.triagegraph.triage.domain.TriageKeyword;

/**
 * Admin-editable keyword rules. Read on every preview, so they are cached in memory and the cache is dropped on
 * every change. The defaults are written on first use, in every profile.
 */
@Service
public class KeywordRuleService implements KeywordRules {

    private static final Logger log = LoggerFactory.getLogger(KeywordRuleService.class);

    private final KeywordRepository repository;
    private final ServiceCatalog catalog;
    private volatile List<KeywordRule> cache;

    public KeywordRuleService(KeywordRepository repository, ServiceCatalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    @Override
    public List<KeywordRule> all() {
        List<KeywordRule> current = cache;
        if (current == null) {
            current = load();
            cache = current;
        }
        return current;
    }

    public List<KeywordRule> list() {
        return all().stream().sorted(Comparator.comparing(KeywordRule::target).thenComparing(KeywordRule::key)
                .thenComparing(KeywordRule::phrase)).toList();
    }

    /** @throws InvalidInputException unknown category or service, bad phrase, or the rule already exists */
    public KeywordRule add(KeywordRule.Target target, String key, String phrase) {
        if (target == null) {
            throw new InvalidInputException("Choose whether the keyword points at a category or a service.");
        }
        String canonicalKey = canonicalKey(target, key);
        TriageKeyword keyword = new TriageKeyword(target, canonicalKey, phrase);
        KeywordRule rule = keyword.toRule();
        boolean exists = all().stream().anyMatch(r -> r.target() == target && r.key().equals(rule.key())
                && r.phrase().equals(rule.phrase()));
        if (exists) {
            throw new InvalidInputException("'" + rule.phrase() + "' is already a keyword for " + rule.key() + ".");
        }
        KeywordRule saved = repository.save(keyword).toRule();
        cache = null;
        return saved;
    }

    public void remove(long id) {
        if (!repository.existsById(id)) {
            throw new InvalidInputException("No keyword rule with id " + id + ".");
        }
        repository.deleteById(id);
        cache = null;
    }

    private String canonicalKey(KeywordRule.Target target, String key) {
        if (target == KeywordRule.Target.CATEGORY) {
            String code = TriageKeyword.normaliseKey(target, key);
            if (!catalog.categoryCodes().contains(code)) {
                throw new InvalidInputException("Unknown category " + code + ".");
            }
            return code;
        }
        return catalog.byName(key).map(ServiceCatalog.ServiceInfo::name)
                .orElseThrow(() -> new InvalidInputException("Unknown service '" + key + "'."));
    }

    private synchronized List<KeywordRule> load() {
        if (repository.count() == 0) {
            List<KeywordRule> defaults = DefaultKeywordRules.all();
            for (KeywordRule r : defaults) {
                repository.save(new TriageKeyword(r.target(), r.key(), r.phrase()));
            }
            log.info("Wrote {} default triage keyword rules", defaults.size());
        }
        return repository.findAll().stream().map(TriageKeyword::toRule).toList();
    }
}
