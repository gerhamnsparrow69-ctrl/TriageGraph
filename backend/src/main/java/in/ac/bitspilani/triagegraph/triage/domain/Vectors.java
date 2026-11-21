package in.ac.bitspilani.triagegraph.triage.domain;

/** Vector maths for normalised embeddings. */
public final class Vectors {

    private Vectors() {
    }

    /** Returns a unit-length copy. A zero vector stays zero (similarity 0 to everything). */
    public static float[] normalise(float[] v) {
        double sum = 0;
        for (float x : v) {
            sum += (double) x * x;
        }
        float[] out = new float[v.length];
        if (sum == 0) {
            return out;
        }
        double norm = Math.sqrt(sum);
        for (int i = 0; i < v.length; i++) {
            out[i] = (float) (v[i] / norm);
        }
        return out;
    }

    /** Cosine similarity of two normalised vectors, i.e. their dot product, in [-1, 1]. */
    public static double cosine(float[] a, float[] b) {
        if (a.length != b.length) {
            throw new IllegalArgumentException("vector sizes differ: " + a.length + " vs " + b.length);
        }
        double dot = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
        }
        return Math.max(-1.0, Math.min(1.0, dot));
    }

    public static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
