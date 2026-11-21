package in.ac.bitspilani.triagegraph.triage.domain;

import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.catalog;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.triage.domain.KeywordRule.Target;

class KeywordRuleClassifierTest {

    private static final List<KeywordRule> RULES = List.of(
            new KeywordRule(1L, Target.CATEGORY, "APPLICATION", "payment"),
            new KeywordRule(2L, Target.CATEGORY, "APPLICATION", "portal"),
            new KeywordRule(3L, Target.CATEGORY, "NETWORK", "wifi"),
            new KeywordRule(4L, Target.CATEGORY, "AUTHENTICATION", "login"),
            new KeywordRule(5L, Target.CATEGORY, "AUTHENTICATION", "password"),
            new KeywordRule(6L, Target.SERVICE, "Fee Payment App", "fee portal"),
            new KeywordRule(7L, Target.SERVICE, "Hostel Wi-Fi", "hostel"));

    private final KeywordRuleClassifier classifier = new KeywordRuleClassifier(() -> RULES, catalog(Map.of("Fee Payment App", 1L)));

    private static TriageText text(String s) {
        return new TriageText(s, new float[] {0f});
    }

    @Test
    void phrasesPickCategoryAndServiceWithAKnownServiceId() {
        var c = classifier.classify(text("UPI payment failing on the fee portal")).orElseThrow();
        assertEquals("APPLICATION", c.categoryCode());
        assertEquals(0.75, c.confidence(), 1e-9);            // payment 1 + portal 1 → 0.45 + 0.15 × 2
        assertEquals("Fee Payment App", c.serviceName());
        assertEquals(Long.valueOf(1L), c.serviceId());
        assertEquals(0.75, c.serviceConfidence(), 1e-9);     // multi-word alias counts 2
        assertTrue(c.explanation().contains("payment"));
    }

    @Test
    void aTieBetweenTwoCategoriesLowersConfidence() {
        var c = classifier.classify(text("Wifi login fails")).orElseThrow();
        assertEquals(0.45, c.confidence(), 1e-9);            // one hit each: 0.60 − 0.15
    }

    @Test
    void serviceOnlyMatchesStillHelp() {
        var c = classifier.classify(text("Hostel block 5 is dead")).orElseThrow();
        assertNull(c.categoryCode());
        assertEquals("Hostel Wi-Fi", c.serviceName());
        assertNull(c.serviceId());                            // catalog has no id for it
    }

    @Test
    void wordsMustMatchWholeNotAsPartOfAnotherWord() {
        assertTrue(classifier.classify(text("The wifiextender was reset")).isEmpty());
    }
}
