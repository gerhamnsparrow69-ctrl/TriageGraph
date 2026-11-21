package in.ac.bitspilani.triagegraph.assignment.application;

import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.FEE_PAYMENT_APP;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.ananya;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.arjun;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.provider;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.ravi;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.request;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.sneha;
import static in.ac.bitspilani.triagegraph.assignment.AssignmentFixtures.tech;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentStrategyRegistry;
import in.ac.bitspilani.triagegraph.assignment.domain.LoadBalancedStrategy;
import in.ac.bitspilani.triagegraph.assignment.domain.RoundRobinStrategy;
import in.ac.bitspilani.triagegraph.assignment.domain.SkillBasedStrategy;
import in.ac.bitspilani.triagegraph.assignment.domain.SmartScoreStrategy;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.WorkloadExceededException;

class AssignmentServiceImplTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-21T10:00:00Z"), ZoneOffset.UTC);

    private Fakes.Directory directory;
    private Fakes.FixRepo fixes;
    private ExperienceService experience;
    private AssignmentSettingsService settings;
    private AssignmentServiceImpl service;

    @BeforeEach
    void setUp() {
        directory = new Fakes.Directory();
        directory.technicians.addAll(List.of(sneha(1), ananya(3), arjun(0), ravi(2)));
        fixes = new Fakes.FixRepo();
        experience = new ExperienceService(fixes);
        var registry = new AssignmentStrategyRegistry(List.of(new SmartScoreStrategy(id -> "Fee Payment App", 5),
                new SkillBasedStrategy(), new LoadBalancedStrategy(), new RoundRobinStrategy()));
        settings = new AssignmentSettingsService(new Fakes.SettingsRepo(), registry,
                new AssignmentProperties("SMART", 5), CLOCK);
        service = new AssignmentServiceImpl(directory, registry, settings, experience,
                new RequiredSkillResolver(provider(null)));
    }

    @Test
    void autoAssignUsesTheDefaultSmartStrategyAndFixHistory() {
        experience.recordFix(4, FEE_PAYMENT_APP, "Fee Payment App", "HIST-001", CLOCK.instant());
        experience.recordFix(4, FEE_PAYMENT_APP, "Fee Payment App", "HIST-002", CLOCK.instant());
        experience.recordFix(4, FEE_PAYMENT_APP, "Fee Payment App", "HIST-003", CLOCK.instant());
        experience.recordFix(2, FEE_PAYMENT_APP, "Fee Payment App", "HIST-004", CLOCK.instant());
        var pick = service.autoAssign(request("APPLICATION", FEE_PAYMENT_APP, Priority.P1));
        assertEquals("Sneha Reddy", pick.name());
        assertEquals("SMART", pick.strategyKey());
        assertEquals(0.715, pick.score(), 1e-9);
    }

    /** Strategy pattern switched at runtime: the very next assignment uses the new strategy. */
    @Test
    void switchingTheStrategyChangesTheNextAssignment() {
        directory.technicians.clear();
        directory.technicians.addAll(List.of(tech(1, "Busy senior", Set.of("APPLICATION"), 3, 4, 10),
                tech(2, "Idle junior", Set.of("APPLICATION"), 0, 6, 1)));
        assertEquals("Busy senior", service.autoAssign(request("APPLICATION", 9, Priority.P1)).name());

        settings.switchTo("LOAD_BALANCED", 7L);
        assertEquals("Idle junior", service.autoAssign(request("APPLICATION", 9, Priority.P1)).name());
        assertEquals("LOAD_BALANCED", settings.current().getActiveStrategy());
        assertEquals(Long.valueOf(7L), settings.current().getUpdatedBy());
    }

    @Test
    void roundRobinAdvancesOnAssignmentsButNotOnRecommendations() {
        settings.switchTo("ROUND_ROBIN", 7L);
        var req = request("APPLICATION", 1, Priority.P3);
        assertEquals("Ananya Iyer", service.recommend(req, 1).get(0).name()); // lowest id first, preview only
        assertEquals("Ananya Iyer", service.recommend(req, 1).get(0).name());
        assertEquals("Ananya Iyer", service.autoAssign(req).name());
        assertEquals("Sneha Reddy", service.autoAssign(req).name());
        assertEquals("Ananya Iyer", service.autoAssign(req).name());
    }

    @Test
    void recommendReturnsAtMostTopKFromTheActiveStrategy() {
        var top = service.recommend(request("APPLICATION", 1, Priority.P3), 5);
        assertEquals(2, top.size()); // only Sneha and Ananya have APPLICATION
        assertEquals(1, service.recommend(request("APPLICATION", 1, Priority.P3), 1).size());
    }

    @Test
    void compareShowsWhatEveryStrategyWouldPick() {
        var outcomes = service.compare(request("APPLICATION", FEE_PAYMENT_APP, Priority.P2));
        assertEquals(4, outcomes.size());
        assertTrue(outcomes.stream().filter(o -> o.active()).allMatch(o -> o.strategyKey().equals("SMART")));
        outcomes.forEach(o -> assertNotNull(o.pick(), o.strategyKey()));
    }

    @Test
    void compareReportsTheReasonWhenNobodyIsEligible() {
        directory.technicians.clear();
        directory.technicians.add(sneha(6));
        var outcomes = service.compare(request("APPLICATION", 1, Priority.P2));
        outcomes.forEach(o -> {
            assertNull(o.pick());
            assertTrue(o.error().contains("maximum workload"), o.error());
        });
        assertThrows(WorkloadExceededException.class, () -> service.autoAssign(request("APPLICATION", 1, Priority.P2)));
    }

    @Test
    void missingCategoryOrPriorityIsInvalidInput() {
        assertThrows(InvalidInputException.class, () -> service.autoAssign(request(" ", 1, Priority.P2)));
        assertThrows(InvalidInputException.class, () -> service.autoAssign(request("APPLICATION", 1, null)));
        var e = assertThrows(InvalidInputException.class, () -> settings.switchTo("FASTEST", 1L));
        assertTrue(e.getMessage().contains("SMART"), e.getMessage());
        assertThrows(InvalidInputException.class,
                () -> service.recommendWith("FASTEST", request("APPLICATION", 1, Priority.P2), 3));
    }

    @Test
    void catalogCanMapACategoryToADifferentSkill() {
        CatalogQuery catalog = new CatalogQuery() {
            public ServiceRef service(long id) {
                return null;
            }

            public CategoryRef category(String code) {
                return new CategoryRef(code, "Smart classroom", "HARDWARE");
            }

            public List<ServiceRef> services() {
                return List.of();
            }

            public List<CategoryRef> activeCategories() {
                return List.of();
            }
        };
        var resolver = new RequiredSkillResolver(provider(catalog));
        assertEquals("HARDWARE", resolver.requiredSkillFor("smart_classroom"));
        assertEquals("NETWORK", new RequiredSkillResolver(provider(null)).requiredSkillFor("network"));
    }
}
