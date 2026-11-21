package in.ac.bitspilani.triagegraph.reporting.application;

import static in.ac.bitspilani.triagegraph.reporting.domain.Facts.T0;
import static in.ac.bitspilani.triagegraph.reporting.domain.Facts.incident;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaStatus;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;

class IncidentSearchServiceTest {

    private final FakeReadModel data = new FakeReadModel();
    private final IncidentSearchService search =
            new IncidentSearchService(data, Clock.fixed(T0.plus(Duration.ofHours(30)), ZoneOffset.UTC));

    @BeforeEach
    void data() {
        for (int i = 1; i <= 25; i++) {
            data.incidents.add(incident(i).reporter(i % 2 == 0 ? 100 : 101).assignee(i <= 5 ? 200L : 201L)
                    .priority(i == 7 ? Priority.P1 : Priority.P3).created(T0.plus(Duration.ofMinutes(i))).build());
        }
        data.teams = Map.of(300L, Set.of(200L));
    }

    @Test
    void pagesNewestFirstWithTotal() {
        var page = search.search(IncidentSearchCriteria.none(), 1, Role.ADMIN, IncidentSort.NEWEST, 1, 10);
        assertThat(page.total()).isEqualTo(25);
        assertThat(page.items()).hasSize(10);
        assertThat(page.items().getFirst().code()).isEqualTo("INC-1015");
    }

    @Test
    void rolesAreScoped() {
        assertThat(search.search(IncidentSearchCriteria.none(), 100, Role.EMPLOYEE, IncidentSort.NEWEST, 0, 100).total())
                .isEqualTo(12);
        assertThat(search.search(IncidentSearchCriteria.none(), 200, Role.TECHNICIAN, IncidentSort.NEWEST, 0, 100).total())
                .isEqualTo(5);
        assertThat(search.search(IncidentSearchCriteria.none(), 300, Role.TEAM_LEAD, IncidentSort.NEWEST, 0, 100).total())
                .isEqualTo(5);
        assertThat(search.search(IncidentSearchCriteria.none(), 8, Role.IT_MANAGER, IncidentSort.NEWEST, 0, 100).total())
                .isEqualTo(25);
    }

    @Test
    void criteriaNarrowTheResultAndSortByPriority() {
        var criteria = new IncidentSearchCriteria(null, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 20),
                Set.of(IncidentStatus.ASSIGNED), null, "network", 1L, null, SlaStatus.BREACHED, null, false, "wi-fi");
        var page = search.search(criteria, 1, Role.ADMIN, IncidentSort.PRIORITY, 0, 5);
        assertThat(page.total()).isEqualTo(25);
        assertThat(page.items().getFirst().code()).isEqualTo("INC-1007");
        assertThat(page.items().getFirst().slaStatus()).isEqualTo(SlaStatus.BREACHED);
    }

    @Test
    void exportReturnsEveryMatchUnpaged() {
        assertThat(search.export(IncidentSearchCriteria.none(), 1, Role.ADMIN, IncidentSort.OLDEST)).hasSize(25)
                .first().extracting(IncidentRow::code).isEqualTo("INC-1001");
    }

    @Test
    void badPagingAndDatesAreRejected() {
        assertThatThrownBy(() -> search.search(IncidentSearchCriteria.none(), 1, Role.ADMIN, IncidentSort.NEWEST, 0, 101))
                .isInstanceOf(InvalidInputException.class);
        var backwards = new IncidentSearchCriteria(null, LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 20),
                null, null, null, null, null, null, null, null, null);
        assertThatThrownBy(() -> search.search(backwards, 1, Role.ADMIN, IncidentSort.NEWEST, 0, 10))
                .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void optionsListServicesCategoriesAndTechnicians() {
        data.services.add(new in.ac.bitspilani.triagegraph.reporting.domain.ServiceFact(1, "LMS", "HIGH"));
        data.technicians.add(new in.ac.bitspilani.triagegraph.reporting.domain.TechnicianFact(200, "Ravi", "TECHNICIAN", 6, true, null));
        var options = search.options();
        assertThat(options.services()).containsExactly(new IncidentSearchService.Option("1", "LMS"));
        assertThat(options.categories()).containsExactly("NETWORK", "SOFTWARE");
        assertThat(options.technicians()).containsExactly(new IncidentSearchService.Option("200", "Ravi"));
    }

    @Test
    void sortNamesAreParsedLeniently() {
        assertThat(IncidentSort.parse("due-soonest")).isEqualTo(IncidentSort.DUE_SOONEST);
        assertThat(IncidentSort.parse(null)).isEqualTo(IncidentSort.NEWEST);
        assertThatThrownBy(() -> IncidentSort.parse("sideways")).isInstanceOf(InvalidInputException.class);
    }
}
