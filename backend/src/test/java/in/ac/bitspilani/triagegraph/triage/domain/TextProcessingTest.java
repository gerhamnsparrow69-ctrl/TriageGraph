package in.ac.bitspilani.triagegraph.triage.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TextProcessingTest {

    @Test
    void combineJoinsTitleAndDescriptionAsOneSentence() {
        assertEquals("Fee app down. Students cannot pay.", TextNormalizer.combine("  Fee app   down ", "Students cannot pay."));
        assertEquals("Is the LMS down? It shows 500.", TextNormalizer.combine("Is the LMS down?", "It shows 500."));
        assertEquals("Only a title", TextNormalizer.combine("Only a title", null));
        assertEquals(TextNormalizer.MAX_CHARS, TextNormalizer.combine("t", "x".repeat(5000)).length());
    }

    @Test
    void forMatchingKeepsOnlyLowerCaseWordsPaddedWithSpaces() {
        assertEquals(" wi fi down in block 5 ", TextNormalizer.forMatching("Wi-Fi DOWN, in Block-5!"));
    }

    @Test
    void piiMaskerHidesEmailsPhonesAndBitsIds() {
        String masked = PiiMasker.mask("I am 2024A7PS0123H, mail f20240123@hyderabad.bits-pilani.ac.in or call +91 98765 43210.");
        assertEquals("I am [student-id], mail [email] or call [phone].", masked);
        assertFalse(masked.contains("9876"));
    }

    @Test
    void piiMaskerLeavesOrdinaryNumbersAlone() {
        String text = "Error 500 on 3 lab PCs in room 2204 since 10 am";
        assertEquals(text, PiiMasker.mask(text));
    }

    @Test
    void vectorsAreNormalisedAndCosineIsTheirDotProduct() {
        float[] a = Vectors.normalise(new float[] {3, 4});
        assertEquals(0.6, a[0], 1e-6);
        assertEquals(1.0, Vectors.cosine(a, a), 1e-6);
        assertEquals(0.0, Vectors.cosine(Vectors.normalise(new float[] {1, 0}), Vectors.normalise(new float[] {0, 2})), 1e-9);
        assertTrue(Vectors.normalise(new float[] {0, 0})[0] == 0);
    }
}
