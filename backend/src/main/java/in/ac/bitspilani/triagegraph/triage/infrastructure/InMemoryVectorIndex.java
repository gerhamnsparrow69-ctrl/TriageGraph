package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import in.ac.bitspilani.triagegraph.triage.domain.KnowledgeView;
import in.ac.bitspilani.triagegraph.triage.domain.ScoredView;
import in.ac.bitspilani.triagegraph.triage.domain.VectorIndex;
import in.ac.bitspilani.triagegraph.triage.domain.Vectors;

/**
 * Exact (brute-force) cosine search over a {@link ConcurrentHashMap}. For a campus desk (thousands of entries,
 * 384 floats each) a full scan takes about a millisecond and needs no extra database extension. The scale path
 * is a pgvector HNSW index behind the same {@link VectorIndex} interface.
 */
public class InMemoryVectorIndex implements VectorIndex {

    private record Entry(KnowledgeView view, float[] vector) {
    }

    private final Map<Long, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public void put(KnowledgeView view, float[] vector) {
        entries.put(view.docId(), new Entry(view, vector.clone()));
    }

    @Override
    public void updateView(KnowledgeView view) {
        entries.computeIfPresent(view.docId(), (id, e) -> new Entry(view, e.vector()));
    }

    @Override
    public boolean contains(long docId) {
        return entries.containsKey(docId);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public List<ScoredView> nearest(float[] query, int k, Predicate<KnowledgeView> filter) {
        return entries.values().stream()
                .filter(e -> filter.test(e.view()) && e.vector().length == query.length)
                .map(e -> new ScoredView(e.view(), Vectors.cosine(query, e.vector())))
                .sorted(Comparator.comparingDouble(ScoredView::similarity).reversed()
                        .thenComparingLong(s -> s.view().docId()))
                .limit(Math.max(0, k))
                .toList();
    }

    @Override
    public List<KnowledgeView> views(Predicate<KnowledgeView> filter) {
        return entries.values().stream().map(Entry::view).filter(filter)
                .sorted(Comparator.comparingLong(KnowledgeView::docId)).toList();
    }
}
