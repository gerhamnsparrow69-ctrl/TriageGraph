package in.ac.bitspilani.triagegraph.triage.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.triage.domain.KeywordRule.Target;
import in.ac.bitspilani.triagegraph.triage.domain.TriageText;

class TriageAdminServicesTest {

    private final TriageTestRig rig = new TriageTestRig();

    @Test
    void settingsStartFromTheCalibratedDefaultsAndAreCached() {
        var s = rig.settings.current();
        assertEquals(0.62, s.getLikelyThreshold(), 1e-9);
        assertEquals(0.50, s.getPossiblyThreshold(), 1e-9);
        assertEquals(72, s.getWindowHours());
        rig.settings.current();
        assertEquals(1, rig.settingsRepo.saves);
    }

    @Test
    void settingsUpdatesAreValidated() {
        rig.settings.update(0.70, 0.55, 48, false, 7L);
        assertEquals(0.70, rig.settings.policy().likelyThreshold(), 1e-9);
        assertFalse(rig.settings.aiEnabled());
        assertThrows(InvalidInputException.class, () -> rig.settings.update(0.5, 0.6, 48, true, 7L));
        assertThrows(InvalidInputException.class, () -> rig.settings.update(0.7, 0.5, 0, true, 7L));
        assertEquals(0.70, rig.settings.policy().likelyThreshold(), 1e-9, "a rejected update changes nothing");
    }

    @Test
    void defaultKeywordsAreWrittenOnceOnFirstUse() {
        int n = rig.keywords.all().size();
        assertTrue(n > 150, "defaults: " + n);
        rig.keywords.all();
        assertEquals(n, rig.keywordRepo.rows.size());
    }

    /** New category or alias with zero code changes: the next classification already uses it. */
    @Test
    void addedKeywordIsUsedByTheVeryNextClassification() {
        var classifier = new in.ac.bitspilani.triagegraph.triage.domain.KeywordRuleClassifier(rig.keywords, rig.catalog);
        var input = new TriageText("The smartboard in F102 is frozen", new float[] {0f});
        assertTrue(classifier.classify(input).isEmpty());
        rig.keywords.add(Target.CATEGORY, "hardware", "smartboard");
        assertEquals("HARDWARE", classifier.classify(input).orElseThrow().categoryCode());
    }

    @Test
    void keywordValidationRejectsUnknownTargetsDuplicatesAndBadIds() {
        assertThrows(InvalidInputException.class, () -> rig.keywords.add(Target.CATEGORY, "PLUMBING", "pipe"));
        assertThrows(InvalidInputException.class, () -> rig.keywords.add(Target.SERVICE, "Canteen App", "menu"));
        assertThrows(InvalidInputException.class, () -> rig.keywords.add(Target.CATEGORY, "NETWORK", "wifi"));
        assertThrows(InvalidInputException.class, () -> rig.keywords.add(Target.CATEGORY, "NETWORK", "!!!"));
        var added = rig.keywords.add(Target.SERVICE, "lms", "e learning");
        assertEquals("LMS", added.key());
        rig.keywords.remove(added.id());
        assertThrows(InvalidInputException.class, () -> rig.keywords.remove(added.id()));
    }
}
