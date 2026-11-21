package in.ac.bitspilani.triagegraph.assignment.application;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.assignment.domain.ServiceNameLookup;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.exception.ItsmException;

/** Service names from Member 3's catalog, with a neutral fallback when the catalog is missing. */
@Component
public class CatalogServiceNames implements ServiceNameLookup {

    private final ObjectProvider<CatalogQuery> catalog;

    public CatalogServiceNames(ObjectProvider<CatalogQuery> catalog) {
        this.catalog = catalog;
    }

    @Override
    public String nameOf(long serviceId) {
        CatalogQuery query = catalog.getIfAvailable();
        if (query != null) {
            try {
                ServiceRef ref = query.service(serviceId);
                if (ref != null && ref.name() != null) {
                    return ref.name();
                }
            } catch (ItsmException notFound) {
                // fall through
            }
        }
        return "this service";
    }
}
