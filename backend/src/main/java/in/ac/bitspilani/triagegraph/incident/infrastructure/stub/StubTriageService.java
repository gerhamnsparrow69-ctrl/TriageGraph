package in.ac.bitspilani.triagegraph.incident.infrastructure.stub;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ClassificationSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ResolutionSuggestion;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.SimilarIncident;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TriageRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TriageResult;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.TriageService;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Keyword lookup; no duplicate detection or similarity. Replaced by M2's ensemble classifier and vector
 * index (roadmap step 8). Services and categories are resolved by name against whichever
 * {@link CatalogQuery} is active (M3's real catalog, or the stub), never by hard-coded id.
 */
public final class StubTriageService implements TriageService {

    private record Rule(String category, String serviceName, String... keywords) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("SECURITY", "Email", "phishing", "malware", "virus", "hacked"),
            new Rule("APPLICATION", "Fee Payment", "fee", "payment"),
            new Rule("AUTHENTICATION", "SSO", "sso", "login", "password"),
            new Rule("NETWORK", "Hostel Wi-Fi", "wi-fi", "wifi", "network", "internet"),
            new Rule("HARDWARE", "Printer", "printer", "projector"),
            new Rule("APPLICATION", "Email", "email", "mail"),
            new Rule("APPLICATION", "ERP", "erp"));

    private final CatalogQuery catalog;

    public StubTriageService(CatalogQuery catalog) {
        this.catalog = catalog;
    }

    @Override
    public ClassificationSuggestion classify(String text) {
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        List<ServiceRef> services = catalog.services();
        List<CategoryRef> categories = catalog.activeCategories();
        for (Rule r : RULES) {
            for (String k : r.keywords()) {
                Optional<ServiceRef> service = byName(services, r.serviceName());
                if (t.contains(k) && service.isPresent()) {
                    return new ClassificationSuggestion(category(categories, r.category()), service.get().id(), 0.6,
                            "KEYWORD", "Matched keyword \"" + k + "\" (stub classifier)");
                }
            }
        }
        Long fallbackService = services.isEmpty() ? null : services.getFirst().id();
        return new ClassificationSuggestion(category(categories, "APPLICATION"), fallbackService, 0.2, "FALLBACK",
                "No keyword matched (stub)");
    }

    private static Optional<ServiceRef> byName(List<ServiceRef> services, String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return services.stream().filter(s -> s.name().toLowerCase(Locale.ROOT).contains(n)).findFirst();
    }

    /** The rule's category if the catalog has it, else the first active one. */
    private static String category(List<CategoryRef> categories, String code) {
        return categories.stream().map(CategoryRef::code).filter(code::equalsIgnoreCase).findFirst()
                .orElse(categories.isEmpty() ? code : categories.getFirst().code());
    }

    @Override
    public TriageResult preview(TriageRequest req) {
        return new TriageResult(classify(req.title() + "\n" + req.description()), List.of(), List.of());
    }

    @Override
    public void index(long incidentId, String text) {
        // nothing to index without M2's vector store
    }

    @Override
    public void checkNotRepeatSubmission(long reporterId, String text) {
        // duplicate detection arrives with M2
    }

    @Override
    public List<SimilarIncident> similarResolved(long incidentId, int k) {
        return List.of();
    }

    @Override
    public ResolutionSuggestion suggestResolution(long incidentId, boolean useAi) {
        return new ResolutionSuggestion(List.of(), "", false);
    }
}
