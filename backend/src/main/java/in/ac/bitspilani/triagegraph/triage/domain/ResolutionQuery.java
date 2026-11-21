package in.ac.bitspilani.triagegraph.triage.domain;

/** What a resolution advisor needs: the incident text, its vector, the k to return and whether AI may be used. */
public record ResolutionQuery(String text, float[] vector, Long excludeDocId, int k, boolean useAi) {
}
