package in.ac.bitspilani.triagegraph.assignment.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** The assignment endpoints through the full Spring stack. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AssignmentApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    private static JwtRequestPostProcessor as(String role) {
        return jwt().jwt(j -> j.subject("1")).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    /** Strategy pattern at runtime: the admin switches strategy without a restart (switched back afterwards). */
    @Test
    void adminSwitchesTheAssignmentStrategyLive() throws Exception {
        mvc.perform(get("/api/v1/admin/assignment/settings").with(as("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategies.length()").value(4));
        try {
            mvc.perform(put("/api/v1/admin/assignment/settings").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"strategyKey\":\"load_balanced\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.activeStrategy").value("LOAD_BALANCED"))
                    .andExpect(jsonPath("$.updatedBy").value(1));
        } finally {
            mvc.perform(put("/api/v1/admin/assignment/settings").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"strategyKey\":\"SMART\"}")).andExpect(status().isOk());
        }
    }

    @Test
    void unknownStrategyIsBadInputAndOnlyAdminsMaySwitch() throws Exception {
        mvc.perform(put("/api/v1/admin/assignment/settings").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strategyKey\":\"FASTEST\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        mvc.perform(put("/api/v1/admin/assignment/settings").with(as("TECHNICIAN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"strategyKey\":\"SMART\"}"))
                .andExpect(status().isForbidden());
    }

    /** TC12 through the API: a category nobody is skilled in gives 409 TECHNICIAN_UNAVAILABLE. */
    @Test
    void recommendationForASkillNobodyHasIsAConflict() throws Exception {
        mvc.perform(post("/api/v1/assignment/recommendations").with(as("TEAM_LEAD")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryCode\":\"QUANTUM_COMPUTING\",\"serviceId\":1,\"priority\":\"P2\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("TECHNICIAN_UNAVAILABLE"));
    }

    @Test
    void recommendationRequestsAreValidated() throws Exception {
        mvc.perform(post("/api/v1/assignment/recommendations").with(as("TEAM_LEAD")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryCode\":\"\",\"priority\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void employeesCannotSeeWorkloadOrRecommendations() throws Exception {
        mvc.perform(get("/api/v1/assignment/workload/me").with(as("EMPLOYEE"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/assignment/recommendations").with(as("EMPLOYEE")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryCode\":\"NETWORK\",\"serviceId\":1,\"priority\":\"P2\"}"))
                .andExpect(status().isForbidden());
    }
}
