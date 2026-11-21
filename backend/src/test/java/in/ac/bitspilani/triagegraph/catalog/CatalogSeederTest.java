package in.ac.bitspilani.triagegraph.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.catalog.application.CategoryNotFoundException;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.CategoryRepository;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ITServiceRepository;
import in.ac.bitspilani.triagegraph.catalog.seed.CatalogSeeder;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalogintegration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@Transactional
class CatalogSeederTest {

    @Autowired
    CatalogSeeder seeder;

    @Autowired
    ITServiceRepository serviceRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    CatalogQuery catalog;

    @Test
    void seedsAppendixADataAndIsIdempotent() {
        seeder.seed();

        assertThat(serviceRepository.count()).isEqualTo(17);
        assertThat(categoryRepository.count()).isEqualTo(9);

        seeder.seed();

        assertThat(serviceRepository.count()).isEqualTo(17);
        assertThat(categoryRepository.count()).isEqualTo(9);
    }

    @Test
    void mapsServicesAndCategoriesToSharedReferences() {
        seeder.seed();

        ServiceRef feePayment = catalog.services().stream()
                .filter(service -> service.name().equals("Fee Payment App"))
                .findFirst()
                .orElseThrow();

        assertThat(feePayment.code()).isEqualTo("Fee Payment App");
        assertThat(feePayment.criticality().weight()).isEqualTo(4);
        assertThat(catalog.service(feePayment.id())).isEqualTo(feePayment);

        CategoryRef application = catalog.category("APPLICATION");
        assertThat(application.name()).isEqualTo("APPLICATION");
        assertThat(application.defaultSkill()).isNull();
        assertThat(catalog.activeCategories()).hasSize(9);
    }

    @Test
    void unknownCategoryUsesAResourceNotFoundError() {
        seeder.seed();

        assertThatThrownBy(() -> catalog.category("NOT_A_CATEGORY"))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("No category with code NOT_A_CATEGORY");
    }
}
