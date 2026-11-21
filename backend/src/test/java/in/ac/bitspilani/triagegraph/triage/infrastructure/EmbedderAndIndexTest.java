package in.ac.bitspilani.triagegraph.triage.infrastructure;

import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.NOW;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.archived;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.live;
import static in.ac.bitspilani.triagegraph.triage.TriageFixtures.unit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.triage.domain.TextEmbedder;
import in.ac.bitspilani.triagegraph.triage.domain.Vectors;

class EmbedderAndIndexTest {

    private final LexicalHashEmbedder lexical = new LexicalHashEmbedder();

    @Test
    void lexicalEmbedderIsNormalisedAndRanksSharedWordsHigher() {
        float[] a = lexical.embed("Fee payment page not loading for students");
        float[] b = lexical.embed("Students cannot load the fee payments page");
        float[] c = lexical.embed("Printer jam in the library");
        assertEquals(1.0, Vectors.cosine(a, a), 1e-6);
        assertTrue(Vectors.cosine(a, b) > 0.4, "shared words: " + Vectors.cosine(a, b));
        assertTrue(Vectors.cosine(a, c) < 0.1, "unrelated: " + Vectors.cosine(a, c));
        assertEquals(List.of("fee", "payment", "page", "load"), LexicalHashEmbedder.tokens("The fee payments page is not loading"));
    }

    @Test
    void fallbackEmbedderUsesTheFallbackWhenThePreferredOneCannotLoad() {
        TextEmbedder chosen = new FallbackTextEmbedder(() -> {
            throw new UnsatisfiedLinkError("no onnxruntime for this CPU");
        }, lexical);
        assertEquals("lexical hashing (fallback)", chosen.name());
        assertEquals(LexicalHashEmbedder.DIMENSIONS, chosen.embed("hello world").length);
    }

    @Test
    void indexReturnsNearestFirstAppliesFiltersAndKeepsVectorsOnMetadataUpdates() {
        var index = new InMemoryVectorIndex();
        index.put(archived(1, "HIST-1", "APPLICATION", "LMS", "r"), unit(1, 0));
        index.put(archived(2, "HIST-2", "NETWORK", "Hostel Wi-Fi", "r"), unit(1, 1));
        index.put(live(3, 30, "INC-30", IncidentStatus.NEW, 1L, "LMS", NOW, 9L), unit(0, 1));
        var nearest = index.nearest(unit(1, 0.1f), 2, v -> true);
        assertEquals(List.of("HIST-1", "HIST-2"), nearest.stream().map(s -> s.view().code()).toList());
        assertEquals(List.of("INC-30"), index.nearest(unit(1, 0), 5, v -> v.isOpen()).stream().map(s -> s.view().code()).toList());

        index.updateView(live(3, 30, "INC-30", IncidentStatus.RESOLVED, 1L, "LMS", NOW, 9L));
        assertTrue(index.nearest(unit(0, 1), 5, v -> v.isOpen()).isEmpty());
        assertEquals(1.0, index.nearest(unit(0, 1), 1, v -> true).get(0).similarity(), 1e-6); // vector unchanged
        assertEquals(3, index.size());
    }
}
