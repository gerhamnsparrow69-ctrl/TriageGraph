package in.ac.bitspilani.triagegraph.incident.infrastructure.stub;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;

import java.util.List;

/** A handful of the demo services and categories in memory. Replaced by M3's catalog (roadmap step 3). */
public final class StubCatalogQuery implements CatalogQuery {

    static final long FEE_PAYMENT = 1, CAMPUS_SSO = 2, HOSTEL_WIFI = 3, ERP = 4, LAB_PRINTERS = 5, EMAIL = 6;

    private static final List<ServiceRef> SERVICES = List.of(
            new ServiceRef(FEE_PAYMENT, "FEE_PAYMENT", "Fee Payment App", Criticality.MISSION_CRITICAL),
            new ServiceRef(CAMPUS_SSO, "CAMPUS_SSO", "Campus SSO", Criticality.MISSION_CRITICAL),
            new ServiceRef(HOSTEL_WIFI, "HOSTEL_WIFI", "Hostel Wi-Fi", Criticality.HIGH),
            new ServiceRef(ERP, "ERP", "ERP", Criticality.HIGH),
            new ServiceRef(LAB_PRINTERS, "LAB_PRINTERS", "Lab Printers", Criticality.LOW),
            new ServiceRef(EMAIL, "EMAIL", "Campus Email", Criticality.HIGH));

    private static final List<CategoryRef> CATEGORIES = List.of(
            new CategoryRef("APPLICATION", "Application", "APPLICATION"),
            new CategoryRef("NETWORK", "Network", "NETWORK"),
            new CategoryRef("AUTHENTICATION", "Authentication", "AUTHENTICATION"),
            new CategoryRef("HARDWARE", "Hardware", "HARDWARE"),
            new CategoryRef("SECURITY", "Security", "SECURITY"));

    @Override
    public ServiceRef service(long id) {
        return SERVICES.stream().filter(s -> s.id() == id).findFirst()
                .orElseThrow(() -> new ServiceNotFoundException("No service with id " + id));
    }

    @Override
    public CategoryRef category(String code) {
        return CATEGORIES.stream().filter(c -> c.code().equalsIgnoreCase(code)).findFirst()
                .orElseThrow(() -> new InvalidInputException("Unknown category " + code));
    }

    @Override
    public List<ServiceRef> services() {
        return SERVICES;
    }

    @Override
    public List<CategoryRef> activeCategories() {
        return CATEGORIES;
    }
}
