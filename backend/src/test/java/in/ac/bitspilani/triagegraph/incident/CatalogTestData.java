package in.ac.bitspilani.triagegraph.incident;

import in.ac.bitspilani.triagegraph.catalog.domain.Category;
import in.ac.bitspilani.triagegraph.catalog.domain.ITService;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.CategoryRepository;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ITServiceRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;

import java.util.List;

/**
 * The catalog rows the incident integration tests report against. The test profile has no seed data,
 * so this creates them the same way M3's CatalogSeeder does (idempotent, safe to call before each test).
 */
@TestComponent
public class CatalogTestData {

    public static final String FEE_PAYMENT = "Fee Payment App";

    @Autowired
    ITServiceRepository services;

    @Autowired
    CategoryRepository categories;

    public void ensure() {
        if (services.findByCode(FEE_PAYMENT).isEmpty()) {
            services.save(new ITService(FEE_PAYMENT, FEE_PAYMENT, Criticality.MISSION_CRITICAL));
        }
        if (categories.findById("APPLICATION").isEmpty()) {
            categories.save(new Category("APPLICATION", "APPLICATION", null, List.of("app", "portal"), List.of()));
        }
    }
}
