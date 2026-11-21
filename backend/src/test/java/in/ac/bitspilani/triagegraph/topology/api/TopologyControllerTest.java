package in.ac.bitspilani.triagegraph.topology.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import in.ac.bitspilani.triagegraph.shared.contracts.ports.BlastRadiusProvider;
import in.ac.bitspilani.triagegraph.shared.exception.ServiceNotFoundException;
import in.ac.bitspilani.triagegraph.topology.application.GraphRegistry;
import in.ac.bitspilani.triagegraph.topology.domain.DependencyEdge;
import in.ac.bitspilani.triagegraph.topology.domain.ServiceGraph;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TopologyControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GraphRegistry graphs;

    @MockitoBean
    BlastRadiusProvider blastRadius;

    private ServiceGraph graph;

    @BeforeEach
    void setUp() {
        graph = new ServiceGraph(
                Set.of(1L, 2L, 3L, 4L),
                List.of(new DependencyEdge(1, 2), new DependencyEdge(2, 3),
                        new DependencyEdge(4, 2)),
                5);
        when(graphs.currentGraph()).thenReturn(graph);
    }

    @Test
    void exposesCurrentGraphDto() throws Exception {
        mvc.perform(get("/api/v1/topology")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceIds.length()").value(4))
                .andExpect(jsonPath("$.edges.length()").value(3))
                .andExpect(jsonPath("$.edges[0].dependentId").value(1))
                .andExpect(jsonPath("$.edges[0].dependencyId").value(2))
                .andExpect(jsonPath("$.edgeCount").value(3))
                .andExpect(jsonPath("$.version").value(5));
    }

    @Test
    void exposesDirectAndTransitiveDependencies() throws Exception {
        mvc.perform(get("/api/v1/topology/services/1/dependencies")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value(1))
                .andExpect(jsonPath("$.directServiceIds[0]").value(2))
                .andExpect(jsonPath("$.transitiveServiceIds[0]").value(2))
                .andExpect(jsonPath("$.transitiveServiceIds[1]").value(3));
    }

    @Test
    void exposesDirectAndTransitiveDependents() throws Exception {
        mvc.perform(get("/api/v1/topology/services/2/dependents")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TEAM_LEAD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directServiceIds[0]").value(1))
                .andExpect(jsonPath("$.directServiceIds[1]").value(4))
                .andExpect(jsonPath("$.transitiveServiceIds[0]").value(1))
                .andExpect(jsonPath("$.transitiveServiceIds[1]").value(4));
    }

    @Test
    void unknownServiceUsesNotFoundProblem() throws Exception {
        mvc.perform(get("/api/v1/topology/services/999/dependencies")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(ServiceNotFoundException.CODE));
    }

    @Test
    void exposesBlastRadiusThroughImpactAnalyzerContract() throws Exception {
        when(blastRadius.dependentCount(3)).thenReturn(2);

        mvc.perform(get("/api/v1/topology/services/3/blast-radius")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_IT_MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value(3))
                .andExpect(jsonPath("$.dependentCount").value(2));
    }

    @Test
    void rejectsNonPositiveServiceId() throws Exception {
        mvc.perform(get("/api/v1/topology/services/0/dependencies")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_TECHNICIAN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresTechnicianOrAdministrativeRole() throws Exception {
        mvc.perform(get("/api/v1/topology")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/topology"))
                .andExpect(status().isUnauthorized());
    }
}
