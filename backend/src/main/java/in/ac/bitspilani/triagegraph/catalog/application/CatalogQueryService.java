package in.ac.bitspilani.triagegraph.catalog.application;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.catalog.domain.Category;
import in.ac.bitspilani.triagegraph.catalog.domain.ITService;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.CategoryRepository;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ITServiceRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;

@Service
@Transactional(readOnly = true)
public class CatalogQueryService implements CatalogQuery {

    private final ITServiceRepository services;
    private final CategoryRepository categories;

    public CatalogQueryService(ITServiceRepository services, CategoryRepository categories) {
        this.services = services;
        this.categories = categories;
    }

    @Override
    public ServiceRef service(long id) {
        return toRef(services.findById(id)
                .orElseThrow(() -> new ServiceNotFoundException("No service with id " + id)));
    }

    @Override
    public CategoryRef category(String code) {
        return categories.findById(code)
                .map(CatalogQueryService::toRef)
                .orElseThrow(() -> new CategoryNotFoundException(code));
    }

    @Override
    public List<ServiceRef> services() {
        return services.findAll().stream()
                .map(CatalogQueryService::toRef)
                .toList();
    }

    @Override
    public List<CategoryRef> activeCategories() {
        return categories.findByActiveTrue().stream()
                .map(CatalogQueryService::toRef)
                .toList();
    }

    static ServiceRef toRef(ITService service) {
        return new ServiceRef(
                service.getId(),
                service.getCode(),
                service.getName(),
                service.getCriticality());
    }

    static CategoryRef toRef(Category category) {
        return new CategoryRef(
                category.getCode(),
                category.getName(),
                category.getDefaultSkill());
    }
}
