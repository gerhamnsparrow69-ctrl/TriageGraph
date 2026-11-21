package in.ac.bitspilani.triagegraph.reporting.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import in.ac.bitspilani.triagegraph.identity.application.NewUser;
import in.ac.bitspilani.triagegraph.identity.application.UserAdminService;
import in.ac.bitspilani.triagegraph.reporting.infrastructure.JdbcIncidentReadModel;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

/**
 * Search and reports over HTTP on H2, through the real SQL read model. Rows are tagged with a random word so
 * incidents written by other test classes in the same database don't change the counts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportingApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UserAdminService admin;

    @Autowired
    JdbcIncidentReadModel readModel;

    String tag;
    long employee;
    long otherEmployee;
    long technician;
    long service;

    @BeforeEach
    void data() {
        tag = "zq" + UUID.randomUUID().toString().substring(0, 8);
        employee = admin.create(NewUser.employee("Dev Sharma", tag + "a@test.local", "Secret123", "Finance")).getId();
        otherEmployee = admin.create(NewUser.employee("Rahul Verma", tag + "b@test.local", "Secret123", "Hostel")).getId();
        technician = admin.create(new NewUser(Role.TECHNICIAN, "Ravi Kumar", tag + "c@test.local", "Secret123", null,
                Set.of("NETWORK"), 4, 6)).getId();
        jdbc.update("INSERT INTO it_services (code, criticality, name) VALUES (?, 'HIGH', ?)", tag, "Hostel Wi-Fi " + tag);
        service = jdbc.queryForObject("SELECT id FROM it_services WHERE code = ?", Long.class, tag);
        Instant now = Instant.now();
        insert("Hostel Wi-Fi down " + tag, employee, "ASSIGNED", "P1", now.minus(Duration.ofHours(2)), null);
        insert("Printer jam " + tag, otherEmployee, "CLOSED", "P3", now.minus(Duration.ofDays(2)),
                now.minus(Duration.ofDays(1)));
        long joined = jdbc.queryForObject("SELECT MAX(id) FROM incidents", Long.class);
        jdbc.update("INSERT INTO incident_followers (incident_id, user_id) VALUES (?, ?)", joined, employee);
    }

    private void insert(String title, long reporter, String status, String priority, Instant created, Instant resolved) {
        jdbc.update("""
                INSERT INTO incidents (title, description, reporter_id, category_code, service_id, impact, urgency,
                    affected_users, priority, status, assignee_id, escalation_level, reopen_count,
                    classification_source, sla_resolution_due_at, resolved_at, created_at, updated_at, version)
                VALUES (?, ?, ?, 'NETWORK', ?, 'SIGNIFICANT', 'HIGH', 5, ?, ?, ?, 0, 0, 'KNN', ?, ?, ?, ?, 0)
                """, title, title, reporter, service, priority, status, technician,
                Timestamp.from(created.plus(Duration.ofHours(4))), resolved == null ? null : Timestamp.from(resolved),
                Timestamp.from(created), Timestamp.from(created));
    }

    private static RequestPostProcessor as(long id, Role role) {
        return jwt().jwt(j -> j.subject(String.valueOf(id)).claim("roles", List.of(role.name())))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Test
    void readModelJoinsNamesFollowersAndCodes() {
        var mine = readModel.incidents().stream().filter(i -> i.title().endsWith(tag)).toList();
        org.assertj.core.api.Assertions.assertThat(mine).hasSize(2).allSatisfy(i -> {
            org.assertj.core.api.Assertions.assertThat(i.code()).isEqualTo("INC-" + (i.id() + 1000));
            org.assertj.core.api.Assertions.assertThat(i.serviceName()).isEqualTo("Hostel Wi-Fi " + tag);
            org.assertj.core.api.Assertions.assertThat(i.assigneeName()).isEqualTo("Ravi Kumar");
        });
        org.assertj.core.api.Assertions.assertThat(readModel.services()).anyMatch(s -> s.id() == service);
        org.assertj.core.api.Assertions.assertThat(readModel.technicians()).anyMatch(t -> t.id() == technician);
    }

    @Test
    void adminSearchesByTextAndPriority() throws Exception {
        mvc.perform(get("/api/v1/search/incidents").param("q", tag).param("priority", "P1").with(as(1, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Hostel Wi-Fi down " + tag))
                .andExpect(jsonPath("$.items[0].reporterName").value("Dev Sharma"))
                .andExpect(jsonPath("$.items[0].slaStatus").value("ON_TRACK"));
    }

    @Test
    void employeeSeesOwnAndJoinedButNotOthers() throws Exception {
        mvc.perform(get("/api/v1/search/incidents").param("q", tag).with(as(employee, Role.EMPLOYEE)))
                .andExpect(jsonPath("$.total").value(2));
        mvc.perform(get("/api/v1/search/incidents").param("q", tag).param("status", "ASSIGNED")
                        .with(as(otherEmployee, Role.EMPLOYEE)))
                .andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/api/v1/search/incidents").param("q", tag).with(as(technician, Role.TECHNICIAN)))
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    void csvExportIsADownload() throws Exception {
        mvc.perform(get("/api/v1/search/incidents/export").param("q", tag).with(as(1, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"incidents-")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string(containsString("Printer jam " + tag)));
    }

    @Test
    void optionsForTheFilters() throws Exception {
        mvc.perform(get("/api/v1/search/incidents/options").with(as(employee, Role.EMPLOYEE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.services[?(@.label == 'Hostel Wi-Fi " + tag + "')]").exists());
    }

    @Test
    void badInputIs400() throws Exception {
        mvc.perform(get("/api/v1/search/incidents").param("size", "500").with(as(1, Role.ADMIN)))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/search/incidents").param("sort", "sideways").with(as(1, Role.ADMIN)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reportsAreForManagersOnly() throws Exception {
        mvc.perform(get("/api/v1/reports").with(as(1, Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.total").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.trend.days", hasSize(14)))
                .andExpect(jsonPath("$.slaByPriority", hasSize(4)));
        mvc.perform(get("/api/v1/reports/overview").with(as(8, Role.IT_MANAGER))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/reports").with(as(employee, Role.EMPLOYEE))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reports").with(as(technician, Role.TECHNICIAN))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reports")).andExpect(status().isUnauthorized());
    }
}
