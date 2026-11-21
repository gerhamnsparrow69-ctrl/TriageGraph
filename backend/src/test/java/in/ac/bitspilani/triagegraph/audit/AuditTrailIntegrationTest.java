package in.ac.bitspilani.triagegraph.audit;

import com.jayway.jsonpath.JsonPath;
import in.ac.bitspilani.triagegraph.audit.application.AuditService;
import in.ac.bitspilani.triagegraph.identity.application.NewUser;
import in.ac.bitspilani.triagegraph.identity.application.UserAdminService;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.events.NoteAdded;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.DomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import in.ac.bitspilani.triagegraph.incident.CatalogTestData;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The audit trail against the real lifecycle, H2 and the real transaction manager.
 * <p>
 * Deliberately NOT {@code @Transactional}: the audit listener runs BEFORE_COMMIT, so a test-managed
 * transaction that rolls back would never commit, the listener would never fire, and the test would
 * prove nothing. Every request here commits for real, exactly as in production.
 */
@SpringBootTest
@Import(CatalogTestData.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditTrailIntegrationTest {

    static final String PAYMENT_DOWN = """
            {"title":"Fee payment app unavailable","description":"Students cannot complete transactions",
             "impact":"EXTENSIVE","urgency":"CRITICAL","affectedUsers":300}""";

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogTestData catalog;

    @Autowired
    UserAdminService admin;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    DomainEventPublisher events;

    @Autowired
    AuditService audit;

    long dev;
    long adminId;

    @BeforeEach
    void users() {
        catalog.ensure();
        dev = admin.create(NewUser.employee("Dev Sharma", email(), "Secret123", "Finance")).getId();
        adminId = admin.create(new NewUser(Role.ADMIN, "Vikram Singh", email(), "Secret123", null, Set.of(), 0, 0)).getId();
        admin.create(new NewUser(Role.TECHNICIAN, "Sneha Patil", email(), "Secret123", null, Set.of("APPLICATION"), 4, 6));
    }

    /** TC05: full lifecycle NEW → CLOSED gives 9 ordered audit entries with correct from/to states. */
    @Test
    void fullLifecycleProducesNineOrderedEntriesWithCorrectStates() throws Exception {
        long[] ids = reportAndWalkToClosed();
        long id = ids[0];
        long tech = ids[1];

        act(get("/api/v1/incidents/" + id + "/timeline"), dev, Role.EMPLOYEE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[*].action").value(contains("CREATED", "CLASSIFIED", "PRIORITY_SET",
                        "ASSIGNED", "ACKNOWLEDGED", "WORK_STARTED", "RESOLVED", "VERIFIED", "CLOSED")))
                .andExpect(jsonPath("$[*].toState").value(contains("NEW", "CLASSIFIED", null, "ASSIGNED",
                        "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED", "VERIFIED", "CLOSED")))
                .andExpect(jsonPath("$[*].fromState").value(contains(null, "NEW", null, "CLASSIFIED", "ASSIGNED",
                        "ACKNOWLEDGED", "IN_PROGRESS", "RESOLVED", "VERIFIED")))
                .andExpect(jsonPath("$[0].actorId").value(dev))
                .andExpect(jsonPath("$[0].actorRole").value("EMPLOYEE"))
                .andExpect(jsonPath("$[2].description").value(containsString("P1 (score 83.2)")))
                .andExpect(jsonPath("$[3].actorId").doesNotExist())                    // auto-assigned by the system
                .andExpect(jsonPath("$[4].actorId").value(tech))
                .andExpect(jsonPath("$[4].actorRole").value("TECHNICIAN"))
                .andExpect(jsonPath("$[7].actorRole").value("EMPLOYEE"));

        String body = act(get("/api/v1/incidents/" + id + "/timeline"), dev, Role.EMPLOYEE)
                .andReturn().getResponse().getContentAsString();
        List<String> hashes = JsonPath.read(body, "$[*].hash");
        List<String> prevs = JsonPath.read(body, "$[*].prevHash");
        for (int i = 1; i < 9; i++) {
            if (prevs.get(i).equals(hashes.get(i - 1))) {
                continue;
            }
            // other tests' entries may sit between ours in the global chain; never ours out of order
            List<Number> idsInOrder = JsonPath.read(body, "$[*].id");
            assertThat(idsInOrder.get(i).longValue()).isGreaterThan(idsInOrder.get(i - 1).longValue());
        }

        act(get("/api/v1/admin/audit/verify"), adminId, Role.ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.message").value(endsWith("entries verified ✓")));
    }

    /** TC17: a rejected action (resolve while ASSIGNED) leaves no audit entry. */
    @Test
    void rejectedActionsAreNotAudited() throws Exception {
        String body = report().andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        long tech = ((Number) JsonPath.read(body, "$.assigneeId")).longValue();

        act(post("/api/v1/incidents/" + id + "/resolve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"summary\":\"Fixed\"}"), tech, Role.TECHNICIAN)
                .andExpect(status().isConflict());

        act(get("/api/v1/incidents/" + id + "/timeline"), dev, Role.EMPLOYEE)
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[*].action").value(contains("CREATED", "CLASSIFIED", "PRIORITY_SET", "ASSIGNED")));
    }

    /** TC32 (audit part): editing one row directly in the database is caught and that row is named. */
    @Test
    void verifyReportsTheTamperedRow() throws Exception {
        long id = reportAndWalkToClosed()[0];
        Long victim = jdbc.queryForObject(
                "select id from audit_entries where incident_id = ? and action = 'RESOLVED'", Long.class, id);
        String original = jdbc.queryForObject("select description from audit_entries where id = ?", String.class, victim);
        try {
            jdbc.update("update audit_entries set description = ? where id = ?", "Resolved within SLA", victim);

            act(get("/api/v1/admin/audit/verify"), adminId, Role.ADMIN)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.firstBrokenId").value(victim))
                    .andExpect(jsonPath("$.message").value(containsString("Audit entry " + victim)));
        } finally {
            jdbc.update("update audit_entries set description = ? where id = ?", original, victim);
        }
        assertThat(audit.verify().valid()).as("restored row verifies again").isTrue();
    }

    /** BEFORE_COMMIT: a change and its audit entry are saved together or not at all. */
    @Test
    void aRolledBackTransactionLeavesNoAuditEntryAndReleasesTheChainLock() throws Exception {
        long incidentId = 900_000 + (long) (Math.random() * 1000);
        Integer before = countFor(incidentId);

        tx.executeWithoutResult(status -> {
            events.publishAll(List.of(new NoteAdded(incidentId, "INC-X", null, Instant.now(), 1, false)));
            status.setRollbackOnly();
        });
        assertThat(countFor(incidentId)).isEqualTo(before);

        // another thread can still write: the lock was released when the transaction ended
        CompletableFuture.runAsync(() -> tx.executeWithoutResult(s -> events.publishAll(
                List.of(new NoteAdded(incidentId, "INC-X", null, Instant.now(), 2, false))))).get(5, TimeUnit.SECONDS);
        assertThat(countFor(incidentId)).isEqualTo(before + 1);
        assertThat(audit.verify().valid()).isTrue();
    }

    @Test
    void adminEndpointsAreForAdminsAndTimelinesForPeopleWhoMaySeeTheIncident() throws Exception {
        long id = ((Number) JsonPath.read(report().andReturn().getResponse().getContentAsString(), "$.id")).longValue();
        long stranger = admin.create(NewUser.employee("Priya Nair", email(), "Secret123", "Library")).getId();

        act(get("/api/v1/admin/audit/verify"), dev, Role.EMPLOYEE).andExpect(status().isForbidden());
        act(get("/api/v1/admin/audit"), dev, Role.EMPLOYEE).andExpect(status().isForbidden());
        act(get("/api/v1/incidents/" + id + "/timeline"), stranger, Role.EMPLOYEE).andExpect(status().isForbidden());
        act(get("/api/v1/admin/audit?size=5"), adminId, Role.ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
        mvc.perform(get("/api/v1/admin/audit/verify")).andExpect(status().isUnauthorized());
    }

    private long[] reportAndWalkToClosed() throws Exception {
        String body = report().andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        long tech = ((Number) JsonPath.read(body, "$.assigneeId")).longValue();
        String base = "/api/v1/incidents/" + id;
        act(post(base + "/acknowledge"), tech, Role.TECHNICIAN).andExpect(status().isOk());
        act(post(base + "/start"), tech, Role.TECHNICIAN).andExpect(status().isOk());
        act(post(base + "/resolve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"summary\":\"Renewed the TLS certificate\"}"), tech, Role.TECHNICIAN).andExpect(status().isOk());
        act(post(base + "/verify"), dev, Role.EMPLOYEE).andExpect(status().isOk());
        act(post(base + "/close"), tech, Role.TECHNICIAN).andExpect(jsonPath("$.status").value("CLOSED"));
        return new long[] {id, tech};
    }

    private ResultActions report() throws Exception {
        return act(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON).content(PAYMENT_DOWN),
                dev, Role.EMPLOYEE);
    }

    private ResultActions act(MockHttpServletRequestBuilder req, long userId, Role role) throws Exception {
        return mvc.perform(req.with(jwt()
                .jwt(j -> j.subject(String.valueOf(userId)).claim("roles", List.of(role.name())))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private Integer countFor(long incidentId) {
        return jdbc.queryForObject("select count(*) from audit_entries where incident_id = ?", Integer.class, incidentId);
    }

    private static String email() {
        return "a-" + UUID.randomUUID().toString().substring(0, 8) + "@campus.test";
    }
}
