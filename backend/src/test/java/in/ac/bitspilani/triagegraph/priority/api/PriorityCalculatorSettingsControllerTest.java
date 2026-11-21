package in.ac.bitspilani.triagegraph.priority.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PriorityCalculatorSettingsControllerTest {

    private static final String URL = "/api/v1/admin/settings/priority-calculator";

    @Autowired
    MockMvc mvc;

    @Test
    void adminSeesActiveCalculator() throws Exception {
        mvc.perform(get(URL).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeKey").value("WEIGHTED_SCORE"))
                .andExpect(jsonPath("$.available[0].key").value("WEIGHTED_SCORE"))
                .andExpect(jsonPath("$.available[0].description").isNotEmpty());
    }

    @Test
    void nonAdminIsForbidden() throws Exception {
        mvc.perform(get(URL).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousIsUnauthorized() throws Exception {
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }
}
