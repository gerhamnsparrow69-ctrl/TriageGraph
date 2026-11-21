package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.Objects;

/** Incident text and its embedding, computed once and shared by every classifier and the duplicate search. */
public record TriageText(String text, float[] vector) {

    public TriageText {
        Objects.requireNonNull(text);
        Objects.requireNonNull(vector);
    }
}
