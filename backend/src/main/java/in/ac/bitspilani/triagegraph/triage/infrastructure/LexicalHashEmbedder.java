package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import in.ac.bitspilani.triagegraph.triage.domain.TextEmbedder;
import in.ac.bitspilani.triagegraph.triage.domain.TextNormalizer;
import in.ac.bitspilani.triagegraph.triage.domain.Vectors;

/**
 * Fallback embedder with no model file: words and word pairs are hashed into a fixed-size vector
 * (feature hashing) with sublinear term frequency. It understands shared words, not synonyms, so it is
 * weaker than MiniLM, but it keeps duplicate detection and kNN working if the ONNX model cannot load.
 */
public class LexicalHashEmbedder implements TextEmbedder {

    public static final int DIMENSIONS = 2048;

    private static final Set<String> STOP_WORDS = Set.of("a", "an", "the", "is", "are", "was", "were", "be", "been",
            "i", "my", "me", "we", "our", "you", "it", "its", "this", "that", "to", "of", "in", "on", "at", "for",
            "and", "or", "but", "with", "from", "by", "as", "since", "so", "not", "no", "can", "cannot", "cant",
            "do", "does", "did", "dont", "doesnt", "have", "has", "had", "will", "would", "please", "any", "all",
            "every", "some", "very", "just", "now", "today", "again", "still", "when", "after", "before", "there",
            "here", "get", "got", "keeps", "keep", "shows", "show", "showing", "even", "also", "only");

    @Override
    public float[] embed(String text) {
        List<String> tokens = tokens(text);
        float[] v = new float[DIMENSIONS];
        for (int i = 0; i < tokens.size(); i++) {
            add(v, tokens.get(i), 1.0f);
            if (i + 1 < tokens.size()) {
                add(v, tokens.get(i) + "_" + tokens.get(i + 1), 0.5f);
            }
        }
        for (int d = 0; d < v.length; d++) {
            if (v[d] > 0) {
                v[d] = (float) (1 + Math.log(v[d]));
            }
        }
        return Vectors.normalise(v);
    }

    @Override
    public String name() {
        return "lexical hashing (fallback)";
    }

    static List<String> tokens(String text) {
        List<String> out = new ArrayList<>();
        for (String raw : TextNormalizer.forMatching(text).trim().split(" ")) {
            if (raw.length() < 2 || STOP_WORDS.contains(raw)) {
                continue;
            }
            out.add(stem(raw));
        }
        return out;
    }

    /** Very light stemming so "payments", "payment" and "paying" meet. */
    static String stem(String w) {
        if (w.length() > 5 && w.endsWith("ing")) {
            return w.substring(0, w.length() - 3);
        }
        if (w.length() > 4 && w.endsWith("ed")) {
            return w.substring(0, w.length() - 2);
        }
        if (w.length() > 3 && w.endsWith("s") && !w.endsWith("ss")) {
            return w.substring(0, w.length() - 1);
        }
        return w;
    }

    private static void add(float[] v, String feature, float weight) {
        int h = feature.hashCode();
        int index = Math.floorMod(h, DIMENSIONS);
        v[index] += weight;
    }
}
