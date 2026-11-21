package in.ac.bitspilani.triagegraph.incident.infrastructure.stub;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.AssignmentRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;
import in.ac.bitspilani.triagegraph.shared.exception.TechnicianUnavailableException;
import in.ac.bitspilani.triagegraph.shared.exception.WorkloadExceededException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IncidentFallbacksTest {

    static final AssignmentRequest REQ = new AssignmentRequest(1, "APPLICATION", 1, Priority.P1);

    @Test
    void fixedSlaTargetsGetLooserWithPriority() {
        FixedSlaService sla = new FixedSlaService();

        assertThat(sla.targetsFor(1, Priority.P1).response()).isEqualTo(Duration.ofMinutes(15));
        assertThat(sla.targetsFor(1, Priority.P4).resolution()).isEqualTo(Duration.ofHours(72));
        assertThat(sla.targetsFor(1, Priority.P2).warningPercent()).isEqualTo(75);
    }

    @Test
    void stubCatalogKnowsTheDemoServices() {
        StubCatalogQuery catalog = new StubCatalogQuery();

        assertThat(catalog.service(1).criticality()).isEqualTo(Criticality.MISSION_CRITICAL);
        assertThat(catalog.category("network").code()).isEqualTo("NETWORK");
        assertThatThrownBy(() -> catalog.service(99)).isInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    void stubTriageMatchesKeywords() {
        StubTriageService triage = new StubTriageService(new StubCatalogQuery());

        assertThat(triage.classify("Fee payment app unavailable").serviceId()).isEqualTo(StubCatalogQuery.FEE_PAYMENT);
        assertThat(triage.classify("Hostel WiFi down").categoryCode()).isEqualTo("NETWORK");
        assertThat(triage.classify("Phishing email received").categoryCode()).isEqualTo("SECURITY");
        assertThat(triage.classify("something odd").source()).isEqualTo("FALLBACK");
    }

    /** Works against any catalog (e.g. M3's real one, with generated ids and its own names). */
    @Test
    void stubTriageResolvesServicesByNameInWhateverCatalogIsActive() {
        CatalogQuery real = mock(CatalogQuery.class);
        when(real.services()).thenReturn(List.of(new ServiceRef(41, "LMS", "LMS", Criticality.HIGH),
                new ServiceRef(57, "Fee Payment App", "Fee Payment App", Criticality.MISSION_CRITICAL)));
        when(real.activeCategories()).thenReturn(List.of(new CategoryRef("SOFTWARE", "SOFTWARE", null),
                new CategoryRef("APPLICATION", "APPLICATION", null)));
        StubTriageService triage = new StubTriageService(real);

        var fee = triage.classify("Fee payment app unavailable");
        assertThat(fee.serviceId()).isEqualTo(57L);
        assertThat(fee.categoryCode()).isEqualTo("APPLICATION");
        var printer = triage.classify("Printer jammed");          // no printer service, no HARDWARE category
        assertThat(printer.source()).isEqualTo("FALLBACK");
        assertThat(printer.serviceId()).isEqualTo(41L);
        assertThat(printer.categoryCode()).isEqualTo("APPLICATION");

        when(real.services()).thenReturn(List.of());
        assertThat(triage.classify("anything").serviceId()).isNull();   // lifecycle then asks the user to choose
    }

    @Test
    void stubAssignmentPicksTheLeastLoadedTechnicianWithCapacity() {
        UserDirectory users = mock(UserDirectory.class);
        when(users.technicians()).thenReturn(List.of(
                tech(1, 3, 4, true), tech(2, 1, 6, true), tech(3, 0, 6, false), tech(4, 1, 6, true)));

        TechnicianRecommendation pick = new StubAssignmentService(users).autoAssign(REQ);

        assertThat(pick.technicianId()).isEqualTo(2);
        assertThat(pick.strategyKey()).isEqualTo(StubAssignmentService.KEY);
    }

    @Test
    void stubAssignmentFailsLikeTheRealOne() {
        UserDirectory users = mock(UserDirectory.class);
        StubAssignmentService stub = new StubAssignmentService(users);

        when(users.technicians()).thenReturn(List.of(tech(1, 0, 6, false)));
        assertThatThrownBy(() -> stub.autoAssign(REQ)).isInstanceOf(TechnicianUnavailableException.class);

        when(users.technicians()).thenReturn(List.of(tech(1, 4, 4, true)));
        assertThatThrownBy(() -> stub.autoAssign(REQ)).isInstanceOf(WorkloadExceededException.class);
    }

    private static TechnicianProfile tech(long id, int load, int max, boolean available) {
        return new TechnicianProfile(id, "T" + id, Set.of("APPLICATION"), load, max, 3, Map.of(), available);
    }
}
