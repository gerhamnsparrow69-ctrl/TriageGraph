package in.ac.bitspilani.triagegraph.topology.seed;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.catalog.domain.ITService;
import in.ac.bitspilani.triagegraph.catalog.domain.ServiceDependency;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ITServiceRepository;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ServiceDependencyRepository;
import in.ac.bitspilani.triagegraph.seed.DataSeeder;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;
import in.ac.bitspilani.triagegraph.topology.domain.DependencyEdge;

@Component
@Profile({"dev", "demo"})
public class TopologySeeder implements DataSeeder {

    private final ITServiceRepository services;
    private final ServiceDependencyRepository dependencies;

    public TopologySeeder(ITServiceRepository services, ServiceDependencyRepository dependencies) {
        this.services = services;
        this.dependencies = dependencies;
    }

    @Override
    public int order() {
        return 11;
    }

    @Override
    @Transactional
    public void seed() {
        Map<String, ITService> byCode = new HashMap<>();
        services.findAll().forEach(service -> byCode.put(service.getCode(), service));

        Set<DependencyEdge> existing = new HashSet<>();
        dependencies.findAll().forEach(edge -> existing.add(new DependencyEdge(
                edge.getDependent().getId(), edge.getDependency().getId())));

        for (DependencySeed seed : EDGES) {
            ITService dependent = requireService(byCode, seed.dependent());
            ITService dependency = requireService(byCode, seed.dependency());
            DependencyEdge edge = new DependencyEdge(dependent.getId(), dependency.getId());
            if (existing.add(edge)) {
                dependencies.save(new ServiceDependency(dependent, dependency));
            }
        }
    }

    private ITService requireService(Map<String, ITService> services, String code) {
        ITService service = services.get(code);
        if (service == null) {
            throw new ServiceNotFoundException("Missing catalogue service " + code);
        }
        return service;
    }

    private record DependencySeed(String dependent, String dependency) {
    }

    private static final List<DependencySeed> EDGES = List.of(
            new DependencySeed("Fee Payment App", "Payment Gateway API"),
            new DependencySeed("Fee Payment App", "Campus SSO"),
            new DependencySeed("Fee Payment App", "Database Cluster"),
            new DependencySeed("Payment Gateway API", "Core Campus Network"),
            new DependencySeed("Payment Gateway API", "DNS/DHCP"),
            new DependencySeed("LMS", "Campus SSO"),
            new DependencySeed("LMS", "Database Cluster"),
            new DependencySeed("LMS", "Cloud Storage"),
            new DependencySeed("ERP Portal", "Campus SSO"),
            new DependencySeed("ERP Portal", "Database Cluster"),
            new DependencySeed("Email & Collaboration", "Campus SSO"),
            new DependencySeed("Email & Collaboration", "DNS/DHCP"),
            new DependencySeed("Library Portal", "Campus SSO"),
            new DependencySeed("Hostel Wi-Fi", "Core Campus Network"),
            new DependencySeed("Hostel Wi-Fi", "Campus SSO"),
            new DependencySeed("Academic Wi-Fi", "Core Campus Network"),
            new DependencySeed("Academic Wi-Fi", "Campus SSO"),
            new DependencySeed("Lab Workstations", "Campus SSO"),
            new DependencySeed("Lab Workstations", "Core Campus Network"),
            new DependencySeed("Print Services", "Core Campus Network"),
            new DependencySeed("Campus SSO", "LDAP Directory"),
            new DependencySeed("Campus SSO", "Core Campus Network"),
            new DependencySeed("LDAP Directory", "Data-Centre Power"),
            new DependencySeed("LDAP Directory", "Core Campus Network"),
            new DependencySeed("Database Cluster", "Data-Centre Power"),
            new DependencySeed("Database Cluster", "Core Campus Network"),
            new DependencySeed("Cloud Storage", "Core Campus Network"),
            new DependencySeed("DNS/DHCP", "Core Campus Network"),
            new DependencySeed("Core Campus Network", "Data-Centre Power"));
}
