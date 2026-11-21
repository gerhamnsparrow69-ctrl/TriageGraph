package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.Locale;
import java.util.Objects;

import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Stored form of a {@link KeywordRule}. */
@Entity
@Table(name = "triage_keyword", uniqueConstraints = @UniqueConstraint(name = "uk_triage_keyword",
        columnNames = {"target", "target_key", "phrase"}))
public class TriageKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private KeywordRule.Target target;

    @Column(name = "target_key", nullable = false, length = 100)
    private String targetKey;

    @Column(nullable = false, length = 60)
    private String phrase;

    protected TriageKeyword() {
    }

    public TriageKeyword(KeywordRule.Target target, String targetKey, String phrase) {
        this.target = Objects.requireNonNull(target);
        this.targetKey = normaliseKey(target, targetKey);
        this.phrase = normalisePhrase(phrase);
    }

    public static String normalisePhrase(String phrase) {
        String p = TextNormalizer.forMatching(phrase).trim();
        if (p.isEmpty() || p.length() > 60) {
            throw new InvalidInputException("A keyword must have 1 to 60 letters or digits.");
        }
        return p;
    }

    public static String normaliseKey(KeywordRule.Target target, String key) {
        String k = TextNormalizer.clean(key);
        if (k.isEmpty()) {
            throw new InvalidInputException("A keyword needs a category or service.");
        }
        return target == KeywordRule.Target.CATEGORY ? k.toUpperCase(Locale.ROOT) : k;
    }

    public KeywordRule toRule() {
        return new KeywordRule(id, target, targetKey, phrase);
    }

    public Long getId() {
        return id;
    }
}
