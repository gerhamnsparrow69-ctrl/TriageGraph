package in.ac.bitspilani.triagegraph.topology.application;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import in.ac.bitspilani.triagegraph.shared.contracts.events.CatalogChanged;

@Component
public class GraphReloadListener {

    private final GraphRegistry registry;

    public GraphReloadListener(GraphRegistry registry) {
        this.registry = registry;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onCatalogChanged(CatalogChanged event) {
        registry.reload();
    }
}
