package in.ac.bitspilani.triagegraph.shared.contracts.ports;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import java.util.List;

/** Provided by Member 3 (catalog). */
public interface CatalogQuery {

    ServiceRef service(long id);

    CategoryRef category(String code);

    List<ServiceRef> services();

    List<CategoryRef> activeCategories();
}
