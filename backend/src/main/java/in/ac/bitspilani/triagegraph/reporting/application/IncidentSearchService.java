package in.ac.bitspilani.triagegraph.reporting.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.reporting.domain.IncidentFact;
import in.ac.bitspilani.triagegraph.reporting.domain.IncidentSpecification;
import in.ac.bitspilani.triagegraph.reporting.domain.IncidentSpecifications;
import in.ac.bitspilani.triagegraph.reporting.domain.Viewer;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;

/**
 * Incident search: role scoping first (employees see their own and joined incidents, technicians their
 * assigned ones, team leads their team's, admins and the IT manager everything), then the criteria, then
 * sort and page.
 */
@Service
public class IncidentSearchService {

    public static final int MAX_PAGE_SIZE = 100;

    private final IncidentReadModel readModel;
    private final Clock clock;

    public IncidentSearchService(IncidentReadModel readModel, Clock clock) {
        this.readModel = readModel;
        this.clock = clock;
    }

    public SearchPage search(IncidentSearchCriteria criteria, long userId, Role role, IncidentSort sort, int page,
            int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new InvalidInputException("Page must be 0 or more and size between 1 and " + MAX_PAGE_SIZE + ".");
        }
        Instant now = clock.instant();
        List<IncidentFact> matches = matching(criteria, userId, role, sort, now);
        List<IncidentRow> rows = matches.stream()
                .skip((long) page * size)
                .limit(size)
                .map(i -> IncidentRow.from(i, now))
                .toList();
        return new SearchPage(rows, page, size, matches.size());
    }

    /** Everything that matches, unpaged, for CSV export. */
    public List<IncidentRow> export(IncidentSearchCriteria criteria, long userId, Role role, IncidentSort sort) {
        Instant now = clock.instant();
        return matching(criteria, userId, role, sort, now).stream().map(i -> IncidentRow.from(i, now)).toList();
    }

    private List<IncidentFact> matching(IncidentSearchCriteria criteria, long userId, Role role, IncidentSort sort,
            Instant now) {
        Viewer viewer = role == Role.TEAM_LEAD
                ? new Viewer(userId, role, readModel.teamMembers(userId))
                : Viewer.of(userId, role);
        IncidentSpecification spec = IncidentSpecifications.visibleTo(viewer).and(criteria.toSpecification(now));
        return readModel.incidents().stream()
                .filter(spec::isSatisfiedBy)
                .sorted(sort.comparator())
                .toList();
    }

    /** What the filter dropdowns offer: services, categories and the people work can be assigned to. */
    public SearchOptions options() {
        return new SearchOptions(
                readModel.services().stream().map(sv -> new Option(String.valueOf(sv.id()), sv.name())).toList(),
                readModel.categories(),
                readModel.technicians().stream().map(t -> new Option(String.valueOf(t.id()), t.name())).toList());
    }

    public record Option(String value, String label) {
    }

    public record SearchOptions(List<Option> services, List<String> categories, List<Option> technicians) {
    }

    public record SearchPage(List<IncidentRow> items, int page, int size, long total) {
    }
}
