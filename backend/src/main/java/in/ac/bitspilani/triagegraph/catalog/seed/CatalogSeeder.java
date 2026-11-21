package in.ac.bitspilani.triagegraph.catalog.seed;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.catalog.domain.Category;
import in.ac.bitspilani.triagegraph.catalog.domain.ITService;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.CategoryRepository;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ITServiceRepository;
import in.ac.bitspilani.triagegraph.seed.DataSeeder;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;

@Component
@Profile({"dev", "demo"})
public class CatalogSeeder implements DataSeeder {

    private final ITServiceRepository services;
    private final CategoryRepository categories;

    public CatalogSeeder(ITServiceRepository services, CategoryRepository categories) {
        this.services = services;
        this.categories = categories;
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    @Transactional
    public void seed() {
        seedServices();
        seedCategories();
    }

    private void seedServices() {
        for (SeedService seed : SERVICES) {
            if (services.findByCode(seed.code()).isEmpty()) {
                services.save(new ITService(seed.code(), seed.name(), seed.criticality()));
            }
        }
    }

    private void seedCategories() {
        for (SeedCategory seed : CATEGORIES) {
            if (categories.findById(seed.code()).isEmpty()) {
                categories.save(new Category(
                        seed.code(),
                        seed.name(),
                        null,
                        seed.keywords(),
                        List.of()));
            }
        }
    }

    private record SeedService(String code, String name, Criticality criticality) {
    }

    private record SeedCategory(String code, String name, List<String> keywords) {
    }

    private static final List<SeedService> SERVICES = List.of(
            new SeedService("Fee Payment App", "Fee Payment App", Criticality.MISSION_CRITICAL),
            new SeedService("Payment Gateway API", "Payment Gateway API", Criticality.MISSION_CRITICAL),
            new SeedService("Campus SSO", "Campus SSO", Criticality.MISSION_CRITICAL),
            new SeedService("LDAP Directory", "LDAP Directory", Criticality.MISSION_CRITICAL),
            new SeedService("Core Campus Network", "Core Campus Network", Criticality.MISSION_CRITICAL),
            new SeedService("Data-Centre Power", "Data-Centre Power", Criticality.MISSION_CRITICAL),
            new SeedService("DNS/DHCP", "DNS/DHCP", Criticality.MISSION_CRITICAL),
            new SeedService("Database Cluster", "Database Cluster", Criticality.MISSION_CRITICAL),
            new SeedService("LMS", "LMS", Criticality.HIGH),
            new SeedService("ERP Portal", "ERP Portal", Criticality.HIGH),
            new SeedService("Email & Collaboration", "Email & Collaboration", Criticality.HIGH),
            new SeedService("Hostel Wi-Fi", "Hostel Wi-Fi", Criticality.HIGH),
            new SeedService("Academic Wi-Fi", "Academic Wi-Fi", Criticality.HIGH),
            new SeedService("Library Portal", "Library Portal", Criticality.MEDIUM),
            new SeedService("Cloud Storage", "Cloud Storage", Criticality.MEDIUM),
            new SeedService("Lab Workstations", "Lab Workstations", Criticality.MEDIUM),
            new SeedService("Print Services", "Print Services", Criticality.LOW));

    private static final List<SeedCategory> CATEGORIES = List.of(
            new SeedCategory("HARDWARE", "HARDWARE",
                    List.of("laptop", "desktop", "monitor", "keyboard", "mouse", "projector", "battery", "screen", "hardware")),
            new SeedCategory("SOFTWARE", "SOFTWARE",
                    List.of("install", "licence", "crash", "update", "software", "office", "antivirus")),
            new SeedCategory("NETWORK", "NETWORK",
                    List.of("wifi", "wi-fi", "internet", "lan", "ethernet", "network", "router", "slow connection", "packet loss")),
            new SeedCategory("DATABASE", "DATABASE",
                    List.of("database", "query", "sql", "deadlock", "replication", "db timeout")),
            new SeedCategory("CLOUD", "CLOUD",
                    List.of("cloud", "vm", "instance", "bucket", "storage quota")),
            new SeedCategory("SECURITY", "SECURITY",
                    List.of("phishing", "malware", "virus", "breach", "suspicious", "hacked", "ransomware", "spam")),
            new SeedCategory("AUTHENTICATION", "AUTHENTICATION",
                    List.of("login", "log in", "password", "sso", "otp", "locked out", "sign in", "2fa", "credentials")),
            new SeedCategory("APPLICATION", "APPLICATION",
                    List.of("portal", "app", "page not loading", "error 500", "erp", "lms", "payment", "checkout", "transaction")),
            new SeedCategory("STORAGE", "STORAGE",
                    List.of("disk", "quota", "file share", "backup", "nas", "storage full")));
}
