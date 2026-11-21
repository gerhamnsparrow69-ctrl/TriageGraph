package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import in.ac.bitspilani.triagegraph.triage.domain.TextEmbedder;

/**
 * Chooses the embedder once, at startup: the preferred one (MiniLM) if it loads, otherwise the fallback.
 * Choosing once matters, because vectors from two different embedders cannot be compared.
 */
public class FallbackTextEmbedder implements TextEmbedder {

    private static final Logger log = LoggerFactory.getLogger(FallbackTextEmbedder.class);

    private final TextEmbedder delegate;

    public FallbackTextEmbedder(Supplier<TextEmbedder> preferred, TextEmbedder fallback) {
        TextEmbedder chosen;
        try {
            chosen = preferred.get();
            chosen.embed("warm up the model");
        } catch (Throwable e) { // includes UnsatisfiedLinkError from the ONNX runtime
            log.warn("Preferred embedder unavailable ({}); using {}", e.toString(), fallback.name());
            chosen = fallback;
        }
        this.delegate = chosen;
        log.info("Triage embedder: {}", delegate.name());
    }

    @Override
    public float[] embed(String text) {
        return delegate.embed(text);
    }

    @Override
    public String name() {
        return delegate.name();
    }
}
