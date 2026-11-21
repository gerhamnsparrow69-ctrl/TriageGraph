package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.List;
import java.util.function.Predicate;

/**
 * In-memory nearest-neighbour search over the knowledge base. The database is the durable copy; this index
 * is rebuilt from it at startup. Swapping in pgvector later means one new implementation of this interface.
 */
public interface VectorIndex {

    void put(KnowledgeView view, float[] vector);

    /** Replaces the metadata of an entry without re-embedding it (status or category changed). */
    void updateView(KnowledgeView view);

    boolean contains(long docId);

    int size();

    /** The k most similar entries that pass the filter, most similar first. */
    List<ScoredView> nearest(float[] query, int k, Predicate<KnowledgeView> filter);

    List<KnowledgeView> views(Predicate<KnowledgeView> filter);
}
