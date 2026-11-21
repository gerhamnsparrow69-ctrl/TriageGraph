package in.ac.bitspilani.triagegraph.topology.rca.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import in.ac.bitspilani.triagegraph.topology.rca.application.CoverageSpecificityRanker;
import in.ac.bitspilani.triagegraph.topology.rca.domain.RcaAssessment;
import in.ac.bitspilani.triagegraph.topology.rca.domain.ScoredCandidate;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RcaControllerTest {

    private static final String URL = "/api/v1/rca/rank";

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CoverageSpecificityRanker ranker;

    @BeforeEach
    void setUp() {
        when(ranker.assess(Set.of(3L, 4L))).thenReturn(new RcaAssessment(List.of(
                new ScoredCandidate(11, "Campus SSO", 0.7833333333333333,
                        1.0, 1.0 / 3.0, List.of("Change logged 10 min ago: auth change")),
                new ScoredCandidate(12, "LDAP Directory", 0.675,
                        1.0, 0.3, List.of("Open incident on this service"))), true));
    }

    @Test
    void technicianReceivesRankedCandidatesTopRunnerUpAndAmbiguity() throws Exception {
        mvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN")))
                        .contentType("application/json")
                        .content("{\"affectedServiceIds\":[3,4]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates").isArray())
                .andExpect(jsonPath("$.candidates.length()").value(2))
                .andExpect(jsonPath("$.candidates[0].serviceId").value(11))
                .andExpect(jsonPath("$.candidates[0].score").value(0.7833333333333333))
                .andExpect(jsonPath("$.candidates[0].evidence[0]")
                        .value("Change logged 10 min ago: auth change"))
                .andExpect(jsonPath("$.topCandidate.serviceName").value("Campus SSO"))
                .andExpect(jsonPath("$.runnerUp.serviceName").value("LDAP Directory"))
                .andExpect(jsonPath("$.ambiguous").value(true));

        verify(ranker).assess(Set.of(3L, 4L));
    }

    @Test
    void teamLeadCanAccessAndEmployeeCannot() throws Exception {
        mvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TEAM_LEAD")))
                        .contentType("application/json")
                        .content("{\"affectedServiceIds\":[3,4]}"))
                .andExpect(status().isOk());

        mvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                        .contentType("application/json")
                        .content("{\"affectedServiceIds\":[3,4]}"))
                .andExpect(status().isForbidden());

        mvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))
                        .contentType("application/json")
                        .content("{\"affectedServiceIds\":[3,4]}"))
                .andExpect(status().isOk());

        mvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_IT_MANAGER")))
                        .contentType("application/json")
                        .content("{\"affectedServiceIds\":[3,4]}"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousCallerIsUnauthorized() throws Exception {
        mvc.perform(post(URL)
                        .contentType("application/json")
                        .content("{\"affectedServiceIds\":[3,4]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingOrNullAffectedIdsAreRejectedByValidation() throws Exception {
        mvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN")))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN")))
                        .contentType("application/json")
                        .content("{\"affectedServiceIds\":[null]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void emptyAffectedSetReturnsAnEmptyAssessment() throws Exception {
        when(ranker.assess(Set.of())).thenReturn(RcaAssessment.empty());

        mvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN")))
                        .contentType("application/json")
                        .content("{\"affectedServiceIds\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidates").isEmpty())
                .andExpect(jsonPath("$.ambiguous").value(false))
                .andExpect(jsonPath("$.topCandidate").doesNotExist())
                .andExpect(jsonPath("$.runnerUp").doesNotExist());
    }

    @Test
    void unknownServiceUsesProblemDetailNotFoundResponse() throws Exception {
        when(ranker.assess(Set.of(999L)))
                .thenThrow(new ServiceNotFoundException("No service with id 999 exists in the graph"));

        mvc.perform(post(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN")))
                        .contentType("application/json")
                        .content("{\"affectedServiceIds\":[999]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("No service with id 999 exists in the graph"));
    }
}
