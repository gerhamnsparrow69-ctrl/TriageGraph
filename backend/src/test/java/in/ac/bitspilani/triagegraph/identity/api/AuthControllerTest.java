package in.ac.bitspilani.triagegraph.identity.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import in.ac.bitspilani.triagegraph.identity.application.NewUser;
import in.ac.bitspilani.triagegraph.identity.application.UserAdminService;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String PASSWORD = "Secret123";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserAdminService admin;

    @Test
    void registerCreatesAnEmployeeAndHidesThePassword() throws Exception {
        register(uniqueEmail(), PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    /** TC15: weak password → 400, existing email → 409. */
    @Test
    void registerRejectsWeakPasswordAndDuplicateEmail() throws Exception {
        String email = uniqueEmail();
        register(email, "weak").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        register(email, PASSWORD).andExpect(status().isCreated());
        register(email.toUpperCase(), PASSWORD).andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_IN_USE"));
    }

    @Test
    void registerValidatesFields() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"Secret123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));
    }

    @Test
    void loginReturnsATokenThatWorksForMe() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        String token = token(email, PASSWORD);

        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void unknownEmailAndWrongPasswordGiveTheSameMessage() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        String a = login("nobody-" + email, PASSWORD).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String b = login(email, "Wrong1234").andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        assertThat((String) JsonPath.read(a, "$.detail")).isEqualTo(JsonPath.read(b, "$.detail"));
    }

    /** TC26: five wrong passwords → 401 each, then even the right password → 423. */
    @Test
    void fiveWrongPasswordsLockTheAccount() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        for (int i = 0; i < 5; i++) {
            login(email, "Wrong1234").andExpect(status().isUnauthorized());
        }
        login(email, PASSWORD)
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_LOCKED"))
                .andExpect(jsonPath("$.lockedUntil").exists());
    }

    /** TC27: no token → 401. */
    @Test
    void meWithoutTokenIs401() throws Exception {
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    /** TC23 (identity side): an employee calling an admin endpoint gets 403. */
    @Test
    void employeeCannotUseAdminEndpoints() throws Exception {
        String email = uniqueEmail();
        register(email, PASSWORD);
        String token = token(email, PASSWORD);
        mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(technicianJson(uniqueEmail())))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateATechnicianAndAssignATeamLead() throws Exception {
        String adminEmail = uniqueEmail();
        admin.create(new NewUser(Role.ADMIN, "Vikram Singh", adminEmail, PASSWORD, null, Set.of(), 0, 0));
        long leadId = admin.create(new NewUser(Role.TEAM_LEAD, "Ananya Iyer", uniqueEmail(), PASSWORD, null,
                Set.of("APPLICATION"), 8, 4)).getId();
        String token = token(adminEmail, PASSWORD);

        String created = mvc.perform(post("/api/v1/admin/users").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(technicianJson(uniqueEmail())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("TECHNICIAN"))
                .andExpect(jsonPath("$.technician.skills.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        Number techId = JsonPath.read(created, "$.id");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/v1/admin/technicians/" + techId).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skills\":[\"APPLICATION\"],\"experienceYears\":4,\"maxLoad\":6,"
                                + "\"available\":true,\"teamLeadId\":" + leadId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.technician.teamLeadName").value("Ananya Iyer"));
    }

    @Test
    void adminCanDeactivateOthersButNotThemselves() throws Exception {
        String adminEmail = uniqueEmail();
        long adminId = admin.create(new NewUser(Role.ADMIN, "Vikram Singh", adminEmail, PASSWORD, null, Set.of(), 0, 0))
                .getId();
        String userEmail = uniqueEmail();
        long userId = admin.create(NewUser.employee("Dev Sharma", userEmail, PASSWORD, null)).getId();
        String token = token(adminEmail, PASSWORD);

        mvc.perform(post("/api/v1/admin/users/" + adminId + "/deactivate").header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/admin/users/" + userId + "/deactivate").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
        login(userEmail, PASSWORD).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/users/" + userId + "/activate").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.active").value(true));
        login(userEmail, PASSWORD).andExpect(status().isOk());
    }

    /** Regression: the team lead must be loaded when a technician logs in (open-in-view is off). */
    @Test
    void technicianWithATeamLeadCanLogIn() throws Exception {
        long leadId = admin.create(new NewUser(Role.TEAM_LEAD, "Karthik Rao", uniqueEmail(), PASSWORD, null,
                Set.of("NETWORK"), 9, 4)).getId();
        String email = uniqueEmail();
        long techId = admin.create(new NewUser(Role.TECHNICIAN, "Ravi Kumar", email, PASSWORD, null,
                Set.of("NETWORK"), 5, 6)).getId();
        admin.updateTechnician(techId, Set.of("NETWORK"), 5, 6, true, leadId);

        login(email, PASSWORD).andExpect(status().isOk())
                .andExpect(jsonPath("$.user.technician.teamLeadName").value("Karthik Rao"));
    }

    private ResultActions register(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Dev Sharma\",\"email\":\"" + email + "\",\"password\":\"" + password
                        + "\",\"department\":\"Finance\"}"));
    }

    private ResultActions login(String email, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private String token(String email, String password) throws Exception {
        String body = login(email, password).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private static String technicianJson(String email) {
        return "{\"role\":\"TECHNICIAN\",\"name\":\"Ravi Kumar\",\"email\":\"" + email + "\",\"password\":\"Secret123\","
                + "\"skills\":[\"NETWORK\",\"AUTHENTICATION\"],\"experienceYears\":5,\"maxLoad\":6}";
    }

    private static String uniqueEmail() {
        return "u-" + UUID.randomUUID().toString().substring(0, 8) + "@campus.test";
    }
}
