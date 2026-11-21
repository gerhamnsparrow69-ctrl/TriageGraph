package in.ac.bitspilani.triagegraph.catalog.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import in.ac.bitspilani.triagegraph.catalog.application.CategoryNotFoundException;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.ServiceRef;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    CatalogQuery catalog;

    @Test
    void listsServicesAsResponseDtos() throws Exception {
        when(catalog.services()).thenReturn(List.of(
                new ServiceRef(7, "LMS", "Learning Management", Criticality.HIGH)));

        mvc.perform(get("/api/v1/catalog/services")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].code").value("LMS"))
                .andExpect(jsonPath("$[0].criticality").value("HIGH"));
    }

    @Test
    void getsOneService() throws Exception {
        when(catalog.service(7)).thenReturn(
                new ServiceRef(7, "LMS", "Learning Management", Criticality.HIGH));

        mvc.perform(get("/api/v1/catalog/services/7")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Learning Management"));
    }

    @Test
    void unknownServiceUsesNotFoundProblem() throws Exception {
        when(catalog.service(999)).thenThrow(new ServiceNotFoundException("No service with id 999"));

        mvc.perform(get("/api/v1/catalog/services/999")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ServiceNotFoundException.CODE));
    }

    @Test
    void listsActiveCategoriesAsResponseDtos() throws Exception {
        when(catalog.activeCategories()).thenReturn(List.of(
                new CategoryRef("NETWORK", "Network", "network")));

        mvc.perform(get("/api/v1/catalog/categories")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("NETWORK"))
                .andExpect(jsonPath("$[0].defaultSkill").value("network"));
    }

    @Test
    void getsOneCategory() throws Exception {
        when(catalog.category("NETWORK")).thenReturn(
                new CategoryRef("NETWORK", "Network", "network"));

        mvc.perform(get("/api/v1/catalog/categories/NETWORK")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("NETWORK"))
                .andExpect(jsonPath("$.name").value("Network"));
    }

    @Test
    void unknownCategoryUsesNotFoundProblem() throws Exception {
        when(catalog.category("UNKNOWN")).thenThrow(new CategoryNotFoundException("UNKNOWN"));

        mvc.perform(get("/api/v1/catalog/categories/UNKNOWN")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(CategoryNotFoundException.CODE));
    }

    @Test
    void requiresAuthenticationAndRecognizedRole() throws Exception {
        mvc.perform(get("/api/v1/catalog/services"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/catalog/services")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_UNKNOWN"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidServiceIdFormatIsBadRequest() throws Exception {
        mvc.perform(get("/api/v1/catalog/services/not-a-number")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isBadRequest())
                .andExpect(contentTypeJson());
    }

    private static org.springframework.test.web.servlet.ResultMatcher contentTypeJson() {
        return result -> org.assertj.core.api.Assertions.assertThat(
                result.getResponse().getContentType()).contains(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    }
}
