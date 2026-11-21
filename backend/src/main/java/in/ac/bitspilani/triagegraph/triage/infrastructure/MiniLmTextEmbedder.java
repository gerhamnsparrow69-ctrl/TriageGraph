package in.ac.bitspilani.triagegraph.triage.infrastructure;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import in.ac.bitspilani.triagegraph.triage.domain.TextEmbedder;
import in.ac.bitspilani.triagegraph.triage.domain.Vectors;

/**
 * Adapter over LangChain4j's in-process all-MiniLM-L6-v2 (ONNX, 384 dimensions). The model ships inside the
 * Maven artifact, so it needs no API key and no network: duplicate detection works fully offline.
 */
public class MiniLmTextEmbedder implements TextEmbedder {

    private final EmbeddingModel model;

    public MiniLmTextEmbedder() {
        this.model = new AllMiniLmL6V2EmbeddingModel();
    }

    @Override
    public float[] embed(String text) {
        String input = text == null || text.isBlank() ? " " : text;
        return Vectors.normalise(model.embed(input).content().vector());
    }

    @Override
    public String name() {
        return "all-MiniLM-L6-v2 (in-process ONNX)";
    }
}
