package in.ac.bitspilani.triagegraph.incident.api;

import com.jayway.jsonpath.JsonPath;
import in.ac.bitspilani.triagegraph.identity.application.NewUser;
import in.ac.bitspilani.triagegraph.identity.application.UserAdminService;
import in.ac.bitspilani.triagegraph.incident.application.IncidentFactory;
import in.ac.bitspilani.triagegraph.incident.application.ReportIncidentCommand;
import in.ac.bitspilani.triagegraph.incident.domain.IncidentRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Impact;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Urgency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The documented exception and authorisation cases for the incident module, end to end over HTTP
 * (blueprint §16). Not {@code @Transactional}, so "no audit entry" is checked against committed data.
 */
@SpringBootTest
@Import(CatalogTestData.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentRulesAndAccessTest {

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
    IncidentFactory factory;

    @Autowired
    IncidentRepository incidents;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    JdbcTemplate jdbc;

    long employeeA;
    long employeeB;
    long techA;
    long techB;
    long adminId;

    @BeforeEach
    void users() {
        catalog.ensure();
        employeeA = admin.create(NewUser.employee("Dev Sharma", email(), "Secret123", "Finance")).getId();
        employeeB = admin.create(NewUser.employee("Priya Nair", email(), "Secret123", "Library")).getId();
        techA = admin.create(new NewUser(Role.TECHNICIAN, "Sneha Patil", email(), "Secret123", null,
                Set.of("APPLICATION"), 4, 6)).getId();
        techB = admin.create(new NewUser(Role.TECHNICIAN, "Arjun Mehta", email(), "Secret123", null,
                Set.of("NETWORK"), 2, 6)).getId();
        adminId = admin.create(new NewUser(Role.ADMIN, "Vikram Singh", email(), "Secret123", null, Set.of(), 0, 0)).getId();
    }

    @Test
    @DisplayName("TC16: close() on a NEW incident → 409 InvalidStateTransitionException; state unchanged")
    void tc16CloseOnNewIncident() throws Exception {
        long id = tx.execute(s -> incidents.save(factory.create(new ReportIncidentCommand("Projector flickers",
                "Room 101 projector flickers", Impact.MINOR, Urgency.LOW, 1, null, null), employeeA)).getId());

        act(post("/api/v1/incidents/" + id + "/close"), adminId, Role.ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.detail").value("Cannot close an incident that is NEW. Allowed now: classify, merge."));

        act(get("/api/v1/incidents/" + id), adminId, Role.ADMIN)
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.allowedActions").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.hasItem("CLOSE"))));
        assertThat(auditCount(id)).isZero();
    }

    @Test
    @DisplayName("TC17: resolve() while ASSIGNED (not acknowledged) → 409; no audit entry for the rejected action")
    void tc17ResolveBeforeAcknowledge() throws Exception {
        Reported r = report();
        int before = auditCount(r.id());

        act(post("/api/v1/incidents/" + r.id() + "/resolve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"summary\":\"Restarted the service\"}"), r.assignee(), Role.TECHNICIAN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.detail").value(containsString("ASSIGNED")));

        assertThat(auditCount(r.id())).isEqualTo(before);
        act(get("/api/v1/incidents/" + r.id()), employeeA, Role.EMPLOYEE)
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.resolution").doesNotExist());
    }

    @Test
    @DisplayName("TC24: Employee A requests Employee B's incident → 403")
    void tc24EmployeeCannotSeeAnotherEmployeesIncident() throws Exception {
        Reported r = report();

        act(get("/api/v1/incidents/" + r.id()), employeeB, Role.EMPLOYEE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        act(get("/api/v1/incidents/" + r.id() + "/timeline"), employeeB, Role.EMPLOYEE)
                .andExpect(status().isForbidden());
        act(post("/api/v1/incidents/" + r.id() + "/notes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"me too\"}"), employeeB, Role.EMPLOYEE)
                .andExpect(status().isForbidden());
        act(get("/api/v1/incidents/" + r.id()), employeeA, Role.EMPLOYEE).andExpect(status().isOk());
    }

    @Test
    @DisplayName("TC25: a technician resolves an incident assigned to someone else → 403 UnauthorizedOperationException")
    void tc25TechnicianCannotResolveSomeoneElsesIncident() throws Exception {
        Reported r = report();
        String base = "/api/v1/incidents/" + r.id();
        act(post(base + "/acknowledge"), r.assignee(), Role.TECHNICIAN).andExpect(status().isOk());
        act(post(base + "/start"), r.assignee(), Role.TECHNICIAN).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        long other = r.assignee() == techA ? techB : techA;
        int before = auditCount(r.id());

        act(post(base + "/resolve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"summary\":\"Fixed it\"}"), other, Role.TECHNICIAN)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        assertThat(auditCount(r.id())).isEqualTo(before);
        act(get(base), r.assignee(), Role.TECHNICIAN)
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.allowedActions").value(org.hamcrest.Matchers.hasItem("RESOLVE")));
    }

    record Reported(long id, long assignee) {
    }

    private Reported report() throws Exception {
        String body = act(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON).content(PAYMENT_DOWN),
                employeeA, Role.EMPLOYEE).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new Reported(((Number) JsonPath.read(body, "$.id")).longValue(),
                ((Number) JsonPath.read(body, "$.assigneeId")).longValue());
    }

    private ResultActions act(MockHttpServletRequestBuilder req, long userId, Role role) throws Exception {
        return mvc.perform(req.with(jwt()
                .jwt(j -> j.subject(String.valueOf(userId)).claim("roles", List.of(role.name())))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.name()))));
    }

    private int auditCount(long incidentId) {
        Integer n = jdbc.queryForObject("select count(*) from audit_entries where incident_id = ?", Integer.class, incidentId);
        return n == null ? 0 : n;
    }

    private static String email() {
        return "r-" + UUID.randomUUID().toString().substring(0, 8) + "@campus.test";
    }
}
