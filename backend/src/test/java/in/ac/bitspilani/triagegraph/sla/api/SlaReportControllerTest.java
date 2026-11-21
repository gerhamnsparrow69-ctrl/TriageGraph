package in.ac.bitspilani.triagegraph.sla.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.SlaKind;
import in.ac.bitspilani.triagegraph.sla.domain.SlaClock;
import in.ac.bitspilani.triagegraph.sla.infrastructure.SlaClockRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SlaReportControllerTest {

    private static final String URL = "/api/v1/admin/reports/sla-breaches";
    private static final Instant CREATED_AT = Instant.parse("2026-11-21T09:00:00Z");

    @Autowired
    MockMvc mvc;

    @Autowired
    SlaClockRepository clocks;

    @BeforeEach
    void clearClocks() {
        clocks.deleteAll();
    }

    @Test
    void reportsZeroWhenNoClockHasBreached() throws Exception {
        mvc.perform(get(URL).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseBreachCount").value(0))
                .andExpect(jsonPath("$.resolutionBreachCount").value(0))
                .andExpect(jsonPath("$.incidentsWithBreaches").value(0));
    }

    @Test
    void countsResponseBreachFromPersistedClockState() throws Exception {
        saveBreachedClock(1, SlaKind.RESPONSE);

        mvc.perform(get(URL).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseBreachCount").value(1))
                .andExpect(jsonPath("$.resolutionBreachCount").value(0))
                .andExpect(jsonPath("$.incidentsWithBreaches").value(1));
    }

    @Test
    void countsResolutionBreachFromPersistedClockState() throws Exception {
        saveBreachedClock(2, SlaKind.RESOLUTION);

        mvc.perform(get(URL).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseBreachCount").value(0))
                .andExpect(jsonPath("$.resolutionBreachCount").value(1))
                .andExpect(jsonPath("$.incidentsWithBreaches").value(1));
    }

    @Test
    void countsAnIncidentBreachingBothClocksOnlyOnceInTotal() throws Exception {
        SlaClock bothBreached = new SlaClock(incident(3), CREATED_AT);
        bothBreached.markBreachRaised(SlaKind.RESPONSE);
        bothBreached.markBreachRaised(SlaKind.RESOLUTION);
        clocks.saveAndFlush(bothBreached);

        mvc.perform(get(URL).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseBreachCount").value(1))
                .andExpect(jsonPath("$.resolutionBreachCount").value(1))
                .andExpect(jsonPath("$.incidentsWithBreaches").value(1));
    }

    @Test
    void restrictsReportToAdminsAndItManagers() throws Exception {
        mvc.perform(get(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isForbidden());
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void itManagerCanAccessReport() throws Exception {
        mvc.perform(get(URL)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_IT_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responseBreachCount").value(0))
                .andExpect(jsonPath("$.resolutionBreachCount").value(0))
                .andExpect(jsonPath("$.incidentsWithBreaches").value(0));
    }

    private void saveBreachedClock(long incidentId, SlaKind kind) {
        SlaClock clock = new SlaClock(incident(incidentId), CREATED_AT);
        clock.markBreachRaised(kind);
        clocks.saveAndFlush(clock);
    }

    private IncidentSnapshot incident(long id) {
        return new IncidentSnapshot(
                id, "INC-" + id, "Test incident", "Test description", IncidentStatus.IN_PROGRESS,
                Priority.P1, 100, "APPLICATION", 1, Set.of(), null, null,
                CREATED_AT, CREATED_AT.plusSeconds(300), CREATED_AT.plusSeconds(1800));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor admin() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
}
