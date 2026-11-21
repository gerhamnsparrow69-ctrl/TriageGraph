package in.ac.bitspilani.triagegraph.catalog.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private static final String CATALOG_READ_ROLES =
            "hasAnyRole('EMPLOYEE', 'TECHNICIAN', 'TEAM_LEAD', 'ADMIN', 'IT_MANAGER')";

    private final CatalogQuery catalog;

    public CatalogController(CatalogQuery catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/services")
    @PreAuthorize(CATALOG_READ_ROLES)
    public List<ServiceRef> services() {
        return catalog.services();
    }

    @GetMapping("/services/{id}")
    @PreAuthorize(CATALOG_READ_ROLES)
    public ServiceRef service(@PathVariable long id) {
        return catalog.service(id);
    }

    @GetMapping("/categories")
    @PreAuthorize(CATALOG_READ_ROLES)
    public List<CategoryRef> categories() {
        return catalog.activeCategories();
    }

    @GetMapping("/categories/{code}")
    @PreAuthorize(CATALOG_READ_ROLES)
    public CategoryRef category(@PathVariable String code) {
        return catalog.category(code);
    }
}
