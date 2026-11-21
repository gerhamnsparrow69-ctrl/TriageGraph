package in.ac.bitspilani.triagegraph.topology.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.catalog.infrastructure.ServiceDependencyRepository;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ITServiceRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.BlastRadiusProvider;
import in.ac.bitspilani.triagegraph.topology.application.GraphRegistry;
import in.ac.bitspilani.triagegraph.topology.domain.DependencyEdge;
import in.ac.bitspilani.triagegraph.topology.seed.TopologySeeder;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:topologyintegration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@Transactional
class CatalogGraphSourceTest {

    @Autowired
    GraphRegistry registry;

    @Autowired
    TopologySeeder seeder;

    @Autowired
    ServiceDependencyRepository dependencies;

    @Autowired
    ITServiceRepository services;

    @Autowired
    BlastRadiusProvider impactAnalyzer;

    @Test
    void buildsGraphFromSeededCatalogueAndDependencies() {
        var graph = registry.currentGraph();

        assertThat(graph.serviceIds()).hasSize(17);
        assertThat(graph.edgeCount()).isEqualTo(29);
        assertThat(graph.edges()).containsExactlyInAnyOrderElementsOf(expectedEdges());
        assertThat(dependencies.count()).isEqualTo(29);

        seeder.seed();
        assertThat(dependencies.count()).isEqualTo(29);
        assertThat(registry.reload().edgeCount()).isEqualTo(29);
    }

    @Test
    void computesExpectedCatalogBlastRadius() {
        var graph = registry.currentGraph();

        long feePaymentApp = findId("Fee Payment App");
        assertThat(graph.dependenciesOf(feePaymentApp)).hasSize(7).doesNotContain(feePaymentApp);
        assertThat(impactAnalyzer.dependentCount(findId("Data-Centre Power"))).isEqualTo(16);
        assertThat(impactAnalyzer.dependentCount(findId("Core Campus Network"))).isEqualTo(15);
        assertThat(impactAnalyzer.dependentCount(findId("LDAP Directory"))).isEqualTo(9);
        assertThat(impactAnalyzer.dependentCount(findId("Campus SSO"))).isEqualTo(8);
        assertThat(impactAnalyzer.dependentCount(findId("Database Cluster"))).isEqualTo(3);
        assertThat(impactAnalyzer.dependentCount(findId("DNS/DHCP"))).isEqualTo(3);
        assertThat(impactAnalyzer.dependentCount(findId("Payment Gateway API"))).isEqualTo(1);
        assertThat(impactAnalyzer.dependentCount(findId("Cloud Storage"))).isEqualTo(1);
        assertThat(services.findAll()).hasSize(17);
        assertThat(services.findAll().stream()
                .filter(service -> !Set.of("Data-Centre Power", "Core Campus Network", "LDAP Directory",
                        "Campus SSO", "Database Cluster", "DNS/DHCP", "Payment Gateway API", "Cloud Storage")
                        .contains(service.getCode()))
                .mapToInt(service -> impactAnalyzer.dependentCount(service.getId()))
                .distinct())
                .containsExactly(0);
    }

    private long findId(String code) {
        return services.findByCode(code).orElseThrow().getId();
    }

    private List<DependencyEdge> expectedEdges() {
        return List.of(
                edge("Fee Payment App", "Payment Gateway API"),
                edge("Fee Payment App", "Campus SSO"),
                edge("Fee Payment App", "Database Cluster"),
                edge("Payment Gateway API", "Core Campus Network"),
                edge("Payment Gateway API", "DNS/DHCP"),
                edge("LMS", "Campus SSO"),
                edge("LMS", "Database Cluster"),
                edge("LMS", "Cloud Storage"),
                edge("ERP Portal", "Campus SSO"),
                edge("ERP Portal", "Database Cluster"),
                edge("Email & Collaboration", "Campus SSO"),
                edge("Email & Collaboration", "DNS/DHCP"),
                edge("Library Portal", "Campus SSO"),
                edge("Hostel Wi-Fi", "Core Campus Network"),
                edge("Hostel Wi-Fi", "Campus SSO"),
                edge("Academic Wi-Fi", "Core Campus Network"),
                edge("Academic Wi-Fi", "Campus SSO"),
                edge("Lab Workstations", "Campus SSO"),
                edge("Lab Workstations", "Core Campus Network"),
                edge("Print Services", "Core Campus Network"),
                edge("Campus SSO", "LDAP Directory"),
                edge("Campus SSO", "Core Campus Network"),
                edge("LDAP Directory", "Data-Centre Power"),
                edge("LDAP Directory", "Core Campus Network"),
                edge("Database Cluster", "Data-Centre Power"),
                edge("Database Cluster", "Core Campus Network"),
                edge("Cloud Storage", "Core Campus Network"),
                edge("DNS/DHCP", "Core Campus Network"),
                edge("Core Campus Network", "Data-Centre Power"));
    }

    private DependencyEdge edge(String dependent, String dependency) {
        return new DependencyEdge(findId(dependent), findId(dependency));
    }
}
