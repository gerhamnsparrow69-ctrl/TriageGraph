package in.ac.bitspilani.triagegraph.triage.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import in.ac.bitspilani.triagegraph.catalog.domain.Category;
import in.ac.bitspilani.triagegraph.catalog.domain.ITService;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.CategoryRepository;
import in.ac.bitspilani.triagegraph.catalog.infrastructure.ITServiceRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

/** The triage endpoints through the full Spring stack: security, validation, JSON. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TriageApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ITServiceRepository services;

    @Autowired
    CategoryRepository categories;

    /**
     * The test profile has no seed data and the in-memory database is shared by every test context, so the
     * catalog rows these tests rely on are created here (idempotent), the same way M3's CatalogSeeder does.
     */
    @BeforeEach
    void catalog() {
        for (String code : List.of("HARDWARE", "SOFTWARE", "NETWORK", "DATABASE", "CLOUD", "SECURITY",
                "AUTHENTICATION", "APPLICATION", "STORAGE")) {
            if (categories.findById(code).isEmpty()) {
                categories.save(new Category(code, code, null, List.of(), List.of()));
            }
        }
        for (String name : List.of("Fee Payment App", "Hostel Wi-Fi", "LMS")) {
            if (services.findByCode(name).isEmpty()) {
                services.save(new ITService(name, name, Criticality.HIGH));
            }
        }
    }

    private static JwtRequestPostProcessor as(String role) {
        return jwt().jwt(j -> j.subject("1")).authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void previewSuggestsACategoryWhileAnEmployeeTypes() throws Exception {
        mvc.perform(post("/api/v1/triage/preview").with(as("EMPLOYEE")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Hostel Wi-Fi down\",\"description\":\"No internet in block 5 since morning\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestion.categoryCode").value("NETWORK"))
                .andExpect(jsonPath("$.suggestedServiceName").value("Hostel Wi-Fi"))
                .andExpect(jsonPath("$.votes").isArray())
                .andExpect(jsonPath("$.likelyDuplicates").isArray());
    }

    @Test
    void previewNeedsASignedInUserAndValidInput() throws Exception {
        mvc.perform(post("/api/v1/triage/preview").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/triage/preview").with(as("EMPLOYEE")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + "x".repeat(201) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    /** TC23 (triage side): an employee cannot change admin settings. */
    @Test
    void employeeCannotChangeTriageSettings() throws Exception {
        mvc.perform(put("/api/v1/admin/triage/settings").with(as("EMPLOYEE")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"likelyThreshold\":0.7,\"possiblyThreshold\":0.5,\"windowHours\":72,\"aiEnabled\":true}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminGetsAClearErrorForInvalidThresholds() throws Exception {
        mvc.perform(put("/api/v1/admin/triage/settings").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"likelyThreshold\":0.5,\"possiblyThreshold\":0.6,\"windowHours\":72,\"aiEnabled\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void adminSeesStatusAndTheMeasuredAccuracy() throws Exception {
        mvc.perform(get("/api/v1/admin/triage/status").with(as("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.embedder").isNotEmpty());
        mvc.perform(get("/api/v1/admin/triage/evaluation").with(as("IT_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pairs").value(30))
                .andExpect(jsonPath("$.positives").value(12));
    }

    @Test
    void adminAddsAndRemovesAKeyword() throws Exception {
        String created = mvc.perform(post("/api/v1/admin/triage/keywords").with(as("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"CATEGORY\",\"key\":\"hardware\",\"phrase\":\"Smart-board test\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("HARDWARE"))
                .andExpect(jsonPath("$.phrase").value("smart board test"))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(created, "$.id");
        mvc.perform(delete("/api/v1/admin/triage/keywords/" + id).with(as("ADMIN"))).andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/admin/triage/keywords").with(as("TECHNICIAN")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target\":\"CATEGORY\",\"key\":\"HARDWARE\",\"phrase\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void optionsListTheCategoriesAndServices() throws Exception {
        mvc.perform(get("/api/v1/triage/options").with(as("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories").isArray())
                .andExpect(jsonPath("$.services").isArray());
    }
}
