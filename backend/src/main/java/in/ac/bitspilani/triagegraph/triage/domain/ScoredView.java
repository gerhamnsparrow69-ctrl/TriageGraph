package in.ac.bitspilani.triagegraph.triage.domain;

/** A knowledge-base entry and its cosine similarity to a query. */
public record ScoredView(KnowledgeView view, double similarity) {
}
