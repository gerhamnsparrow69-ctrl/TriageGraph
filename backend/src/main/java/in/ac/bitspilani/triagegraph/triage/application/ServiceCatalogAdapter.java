package in.ac.bitspilani.triagegraph.triage.application;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.triage.domain.ServiceCatalog;
import in.ac.bitspilani.triagegraph.triage.domain.TextNormalizer;

/**
 * Adapter from Member 3's {@link CatalogQuery} to triage's own {@link ServiceCatalog} view. When the catalog is
 * not deployed yet, the campus services and categories from blueprint Appendix A are used (with unknown ids),
 * so classification still works.
 */
@Component
public class ServiceCatalogAdapter implements ServiceCatalog {

    static final List<String> DEFAULT_SERVICES = List.of("Fee Payment App", "Payment Gateway API", "Campus SSO",
            "LDAP Directory", "Core Campus Network", "Data-Centre Power", "DNS/DHCP", "Database Cluster", "LMS",
            "ERP Portal", "Email & Collaboration", "Hostel Wi-Fi", "Academic Wi-Fi", "Library Portal",
            "Cloud Storage", "Lab Workstations", "Print Services");
    static final List<String> DEFAULT_CATEGORIES = List.of("HARDWARE", "SOFTWARE", "NETWORK", "DATABASE", "CLOUD",
            "SECURITY", "AUTHENTICATION", "APPLICATION", "STORAGE");

    private final ObjectProvider<CatalogQuery> catalog;

    public ServiceCatalogAdapter(ObjectProvider<CatalogQuery> catalog) {
        this.catalog = catalog;
    }

    @Override
    public List<ServiceInfo> services() {
        CatalogQuery q = catalog.getIfAvailable();
        if (q != null) {
            try {
                List<ServiceRef> refs = q.services();
                if (refs != null && !refs.isEmpty()) {
                    return refs.stream().map(r -> new ServiceInfo(r.id(), r.name())).toList();
                }
            } catch (RuntimeException e) {
                // fall back to defaults
            }
        }
        return DEFAULT_SERVICES.stream().map(n -> new ServiceInfo(null, n)).toList();
    }

    @Override
    public List<String> categoryCodes() {
        CatalogQuery q = catalog.getIfAvailable();
        if (q != null) {
            try {
                List<CategoryRef> refs = q.activeCategories();
                if (refs != null && !refs.isEmpty()) {
                    return refs.stream().map(CategoryRef::code).toList();
                }
            } catch (RuntimeException e) {
                // fall back to defaults
            }
        }
        return DEFAULT_CATEGORIES;
    }

    @Override
    public Optional<ServiceInfo> byName(String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        String key = TextNormalizer.forMatching(name).trim();
        return services().stream().filter(s -> TextNormalizer.forMatching(s.name()).trim().equals(key)).findFirst();
    }

    @Override
    public Optional<ServiceInfo> byId(long id) {
        return services().stream().filter(s -> s.id() != null && s.id() == id).findFirst();
    }
}
