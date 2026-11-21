package in.ac.bitspilani.triagegraph.sla.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.SlaTargets;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.SlaService;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SlaControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    SlaService sla;

    @Test
    void returnsTargetsForServiceAndPriority() throws Exception {
        when(sla.targetsFor(12, Priority.P1)).thenReturn(
                new SlaTargets(Duration.ofMinutes(5), Duration.ofMinutes(30), 75, "DEFAULT_P1"));

        mvc.perform(get("/api/v1/sla/targets")
                        .param("serviceId", "12")
                        .param("priority", "P1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("PT5M"))
                .andExpect(jsonPath("$.resolution").value("PT30M"))
                .andExpect(jsonPath("$.warningPercent").value(75))
                .andExpect(jsonPath("$.policyName").value("DEFAULT_P1"));

        verify(sla).targetsFor(12, Priority.P1);
    }

    @Test
    void rejectsMissingOrInvalidTargetParameters() throws Exception {
        mvc.perform(get("/api/v1/sla/targets")
                        .param("priority", "P1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/sla/targets")
                        .param("serviceId", "0")
                        .param("priority", "P1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/sla/targets")
                        .param("serviceId", "12")
                        .param("priority", "P0")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownServiceUsesNotFoundProblem() throws Exception {
        when(sla.targetsFor(999, Priority.P1))
                .thenThrow(new ServiceNotFoundException("No service with id 999"));

        mvc.perform(get("/api/v1/sla/targets")
                        .param("serviceId", "999")
                        .param("priority", "P1")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ServiceNotFoundException.CODE));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/sla/targets")
                        .param("serviceId", "12")
                        .param("priority", "P1"))
                .andExpect(status().isUnauthorized());
    }
}
