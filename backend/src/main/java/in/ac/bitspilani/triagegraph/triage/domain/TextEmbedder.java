package in.ac.bitspilani.triagegraph.triage.domain;

/**
 * Turns text into a meaning vector. Implementations are adapters (in-process MiniLM, or a lexical fallback)
 * and must return L2-normalised vectors, so cosine similarity is a plain dot product.
 */
public interface TextEmbedder {

    float[] embed(String text);

    /** Shown on the admin status page, e.g. "all-MiniLM-L6-v2 (in-process ONNX)". */
    String name();
}
