package in.ac.bitspilani.triagegraph.incident.api;

import com.jayway.jsonpath.JsonPath;
import in.ac.bitspilani.triagegraph.identity.application.NewUser;
import in.ac.bitspilani.triagegraph.identity.application.UserAdminService;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import in.ac.bitspilani.triagegraph.incident.CatalogTestData;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Roadmap step 4 over HTTP, with the real priority service and the stub SLA, triage and assignment:
 * report → ASSIGNED, the full lifecycle to CLOSED, and illegal transitions → 409.
 */
@SpringBootTest
@Import(CatalogTestData.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IncidentControllerTest {

    static final String PAYMENT_DOWN = """
            {"title":"Fee payment app unavailable","description":"Students cannot complete transactions",
             "impact":"EXTENSIVE","urgency":"CRITICAL","affectedUsers":300}""";

    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogTestData catalog;

    @Autowired
    UserAdminService admin;

    long dev;
    long otherEmployee;

    @BeforeEach
    void users() {
        catalog.ensure();
        dev = admin.create(NewUser.employee("Dev Sharma", uniqueEmail(), "Secret123", "Finance")).getId();
        otherEmployee = admin.create(NewUser.employee("Priya Nair", uniqueEmail(), "Secret123", "Library")).getId();
        admin.create(new NewUser(Role.TECHNICIAN, "Sneha Patil", uniqueEmail(), "Secret123", null,
                Set.of("APPLICATION"), 4, 6));
    }

    @Test
    void reportingRunsTheWholePipelineAndEndsAssigned() throws Exception {
        report(dev)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/v1/incidents/")))
                .andExpect(jsonPath("$.code").value(containsString("INC-")))
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.categoryCode").value("APPLICATION"))
                .andExpect(jsonPath("$.classificationSource").value("KEYWORD"))
                .andExpect(jsonPath("$.priority").value("P1"))
                .andExpect(jsonPath("$.priorityScore").value(83.2))
                .andExpect(jsonPath("$.assigneeId").value(notNullValue()))
                .andExpect(jsonPath("$.sla.responseDueAt").value(notNullValue()))
                .andExpect(jsonPath("$.allowedActions", contains("ADD_NOTE")));
    }

    /** TC16/TC17 over HTTP: 409 with the error code, and the incident is unchanged. */
    @Test
    void illegalTransitionsReturn409AndChangeNothing() throws Exception {
        String body = report(dev).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        long tech = ((Number) JsonPath.read(body, "$.assigneeId")).longValue();

        perform(post("/api/v1/incidents/" + id + "/resolve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"summary\":\"Fixed\"}"), tech, Role.TECHNICIAN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STATE_TRANSITION"))
                .andExpect(jsonPath("$.detail").value(containsString("ASSIGNED")));
        perform(post("/api/v1/incidents/" + id + "/close"), tech, Role.TECHNICIAN)
                .andExpect(status().isConflict());
        perform(post("/api/v1/incidents/" + id + "/verify"), dev, Role.EMPLOYEE)
                .andExpect(status().isConflict());

        perform(get("/api/v1/incidents/" + id), dev, Role.EMPLOYEE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ASSIGNED"))
                .andExpect(jsonPath("$.resolution").doesNotExist());
    }

    @Test
    void fullLifecycleFromReportToFeedback() throws Exception {
        String body = report(dev).andReturn().getResponse().getContentAsString();
        String base = "/api/v1/incidents/" + JsonPath.read(body, "$.id");
        long tech = ((Number) JsonPath.read(body, "$.assigneeId")).longValue();

        perform(post(base + "/acknowledge"), tech, Role.TECHNICIAN)
                .andExpect(jsonPath("$.status").value("ACKNOWLEDGED"))
                .andExpect(jsonPath("$.sla.respondedAt").value(notNullValue()));
        perform(post(base + "/start"), tech, Role.TECHNICIAN).andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        perform(post(base + "/request-info").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Which payment method?\"}"), tech, Role.TECHNICIAN)
                .andExpect(jsonPath("$.status").value("PENDING_INFO"))
                .andExpect(jsonPath("$.sla.paused").value(true));
        perform(post(base + "/provide-info").contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"UPI\"}"), dev, Role.EMPLOYEE)
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        perform(post(base + "/resolve").contentType(MediaType.APPLICATION_JSON)
                .content("{\"summary\":\"Renewed the TLS certificate\",\"rootCause\":\"Expired\"}"), tech, Role.TECHNICIAN)
                .andExpect(jsonPath("$.status").value("RESOLVED"));
        perform(post(base + "/verify"), dev, Role.EMPLOYEE).andExpect(jsonPath("$.status").value("VERIFIED"));
        perform(post(base + "/close"), tech, Role.TECHNICIAN).andExpect(jsonPath("$.status").value("CLOSED"));
        perform(post(base + "/feedback").contentType(MediaType.APPLICATION_JSON)
                .content("{\"rating\":5,\"comment\":\"Fast\"}"), dev, Role.EMPLOYEE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feedbackRating").value(5))
                .andExpect(jsonPath("$.notes.length()").value(2))
                .andExpect(jsonPath("$.allowedActions").isEmpty());
    }

    @Test
    void otherPeopleGet403() throws Exception {
        String body = report(dev).andReturn().getResponse().getContentAsString();
        long id = ((Number) JsonPath.read(body, "$.id")).longValue();
        long tech = ((Number) JsonPath.read(body, "$.assigneeId")).longValue();

        perform(get("/api/v1/incidents/" + id), otherEmployee, Role.EMPLOYEE)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        perform(post("/api/v1/incidents/" + id + "/acknowledge"), dev, Role.EMPLOYEE)
                .andExpect(status().isForbidden());
        perform(post("/api/v1/incidents/" + id + "/assign").contentType(MediaType.APPLICATION_JSON)
                .content("{\"technicianId\":" + tech + "}"), tech, Role.TECHNICIAN)
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidReportsAre400AndUnknownIncidents404() throws Exception {
        perform(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"description\":\" \",\"impact\":\"MINOR\",\"urgency\":\"LOW\"}"),
                dev, Role.EMPLOYEE)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("description"));
        perform(get("/api/v1/incidents/999999"), dev, Role.EMPLOYEE)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INCIDENT_NOT_FOUND"));
    }

    @Test
    void anonymousCallersGet401() throws Exception {
        mvc.perform(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON).content(PAYMENT_DOWN))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions report(long reporter) throws Exception {
        return perform(post("/api/v1/incidents").contentType(MediaType.APPLICATION_JSON).content(PAYMENT_DOWN),
                reporter, Role.EMPLOYEE);
    }

    private ResultActions perform(MockHttpServletRequestBuilder req, long userId, Role role) throws Exception {
        return mvc.perform(req.with(as(userId, role)));
    }

    private static RequestPostProcessor as(long userId, Role role) {
        return jwt().jwt(j -> j.subject(String.valueOf(userId)).claim(JwtActors.ROLES_CLAIM, List.of(role.name())));
    }

    private static String uniqueEmail() {
        return "i-" + UUID.randomUUID().toString().substring(0, 8) + "@campus.test";
    }
}
