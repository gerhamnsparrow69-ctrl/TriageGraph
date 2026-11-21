package in.ac.bitspilani.triagegraph.incident.infrastructure.stub;

import in.ac.bitspilani.triagegraph.shared.contracts.ports.AssignmentService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.BlastRadiusProvider;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.SlaService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.TriageService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Temporary stand-ins so the lifecycle runs end to end before the real modules land (roadmap step 4).
 * Each bean steps aside as soon as the owning member registers a real implementation of the port as a
 * component ({@code @Service}): M2 triage + assignment (step 8), M3 catalog, topology and SLA (steps 3, 7).
 * Delete this package once all five real implementations are merged.
 */
@Configuration(proxyBeanMethods = false)
class IncidentFallbacksConfig {

    @Bean
    @ConditionalOnMissingBean(CatalogQuery.class)
    CatalogQuery stubCatalogQuery() {
        return new StubCatalogQuery();
    }

    @Bean
    @ConditionalOnMissingBean(BlastRadiusProvider.class)
    BlastRadiusProvider stubBlastRadiusProvider() {
        return serviceId -> 0;
    }

    @Bean
    @ConditionalOnMissingBean(SlaService.class)
    SlaService fixedSlaService() {
        return new FixedSlaService();
    }

    @Bean
    @ConditionalOnMissingBean(TriageService.class)
    TriageService stubTriageService(CatalogQuery catalog) {
        return new StubTriageService(catalog);
    }

    @Bean
    @ConditionalOnMissingBean(AssignmentService.class)
    AssignmentService stubAssignmentService(UserDirectory users) {
        return new StubAssignmentService(users);
    }
}
