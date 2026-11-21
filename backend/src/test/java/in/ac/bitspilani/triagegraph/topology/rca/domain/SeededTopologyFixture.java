package in.ac.bitspilani.triagegraph.topology.rca.domain;

import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import in.ac.bitspilani.triagegraph.topology.domain.DependencyEdge;
import in.ac.bitspilani.triagegraph.topology.domain.ServiceGraph;

final class SeededTopologyFixture {

    private static final List<String> SERVICES = List.of(
            "Fee Payment App",
            "Payment Gateway API",
            "LMS",
            "ERP Portal",
            "Email & Collaboration",
            "Library Portal",
            "Hostel Wi-Fi",
            "Academic Wi-Fi",
            "Lab Workstations",
            "Print Services",
            "Campus SSO",
            "LDAP Directory",
            "Data-Centre Power",
            "Database Cluster",
            "Cloud Storage",
            "DNS/DHCP",
            "Core Campus Network");

    private static final List<String[]> EDGES = List.of(
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

    private static final Map<String, Long> IDS = java.util.stream.IntStream.range(0, SERVICES.size())
            .boxed()
            .collect(toMap(SERVICES::get, index -> index.longValue() + 1));
    private static final Map<Long, String> NAMES = IDS.entrySet().stream()
            .collect(toMap(Map.Entry::getValue, Map.Entry::getKey));

    private final ServiceGraph graph;
    private final ImpactTopology topology;

    SeededTopologyFixture() {
        List<DependencyEdge> edges = new ArrayList<>();
        for (String[] edge : EDGES) {
            edges.add(new DependencyEdge(id(edge[0]), id(edge[1])));
        }
        graph = new ServiceGraph(IDS.values(), edges, 1);
        topology = new ImpactTopology() {
            @Override
            public boolean contains(long serviceId) {
                return graph.contains(serviceId);
            }

            @Override
            public Set<Long> serviceIds() {
                return graph.serviceIds();
            }

            @Override
            public Set<Long> dependenciesOf(long serviceId) {
                return graph.dependenciesOf(serviceId);
            }

            @Override
            public Set<Long> dependentsOf(long serviceId) {
                return graph.dependentsOf(serviceId);
            }

            @Override
            public long version() {
                return graph.version();
            }
        };
    }

    long id(String serviceName) {
        return IDS.get(serviceName);
    }

    String name(long serviceId) {
        return NAMES.get(serviceId);
    }

    ImpactTopology topology() {
        return topology;
    }

    int serviceCount() {
        return graph.serviceIds().size();
    }

    int edgeCount() {
        return graph.edgeCount();
    }

    List<Long> candidates(Set<Long> affected) {
        return new CandidateGenerator().generate(affected, topology);
    }

    RcaAssessment assess(Set<Long> affected, Set<Long> recentChangeServices, Set<Long> openIncidentServices) {
        List<Long> candidateIds = candidates(affected);
        EvidenceAggregator aggregator = new EvidenceAggregator(0.15);
        Map<Long, AggregatedEvidence> evidence = new HashMap<>();
        for (long candidateId : candidateIds) {
            List<Evidence> items = new ArrayList<>();
            if (recentChangeServices.contains(candidateId)) {
                items.add(new Evidence(EvidenceType.RECENT_CHANGE, 0.10, "Change logged 10 min ago: test change"));
            }
            if (openIncidentServices.contains(candidateId)) {
                items.add(new Evidence(EvidenceType.OPEN_INCIDENT, 0.05, "Open incident on this service"));
            }
            evidence.put(candidateId, aggregator.aggregate(items));
        }

        Map<Long, String> names = new HashMap<>();
        candidateIds.forEach(candidateId -> names.put(candidateId, name(candidateId)));
        return new RcaScorer(RcaConfig.defaults()).assess(candidateIds, affected, topology, names, evidence);
    }

    RcaAssessment assess(Set<Long> affected) {
        return assess(affected, Set.of(), Set.of());
    }

    static Set<Long> services(SeededTopologyFixture fixture, String... names) {
        Set<Long> result = new LinkedHashSet<>();
        for (String name : names) {
            result.add(fixture.id(name));
        }
        return Set.copyOf(result);
    }

    private static String[] edge(String dependent, String dependency) {
        return new String[] {dependent, dependency};
    }
}
