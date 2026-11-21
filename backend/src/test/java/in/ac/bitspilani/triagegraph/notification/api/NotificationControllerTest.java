package in.ac.bitspilani.triagegraph.notification.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

import in.ac.bitspilani.triagegraph.identity.application.AuthService;
import in.ac.bitspilani.triagegraph.identity.application.JwtService;
import in.ac.bitspilani.triagegraph.identity.application.NewUser;
import in.ac.bitspilani.triagegraph.identity.application.UserAdminService;
import in.ac.bitspilani.triagegraph.notification.application.Sleeper;
import in.ac.bitspilani.triagegraph.notification.infrastructure.NtfyGateway;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    @TestConfiguration
    static class NoWait {
        @Bean
        @Primary
        Sleeper noWait() {
            return d -> { };
        }
    }

    @MockitoBean
    JavaMailSender mail;

    @MockitoBean
    NtfyGateway ntfy;

    @Autowired
    MockMvc mvc;

    @Autowired
    UserAdminService admin;

    @Autowired
    AuthService auth;

    @Autowired
    JwtService jwt;

    String adminToken;
    String userToken;
    String otherToken;
    long userId;

    @BeforeEach
    void setUp() {
        String adminEmail = unique();
        admin.create(new NewUser(Role.ADMIN, "Vikram Singh", adminEmail, "Secret123", null, Set.of(), 0, 0));
        adminToken = auth.login(adminEmail, "Secret123").token().token();
        String email = unique();
        userId = admin.create(NewUser.employee("Dev Sharma", email, "Secret123", null)).getId();
        userToken = auth.login(email, "Secret123").token().token();
        String other = unique();
        admin.create(NewUser.employee("Rahul Verma", other, "Secret123", null));
        otherToken = auth.login(other, "Secret123").token().token();
    }

    @Test
    void inboxListCountAndMarkRead() throws Exception {
        mvc.perform(post("/api/v1/admin/notifications/test/" + userId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isAccepted());

        mvc.perform(get("/api/v1/notifications/me/unread-count").header("Authorization", bearer(userToken)))
                .andExpect(jsonPath("$.unread").value(1));
        String list = mvc.perform(get("/api/v1/notifications/me").header("Authorization", bearer(userToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].subject").value("Test notification"))
                .andExpect(jsonPath("$[0].read").value(false))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(list, "$[0].id");

        mvc.perform(post("/api/v1/notifications/" + id + "/read").header("Authorization", bearer(otherToken)))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/notifications/" + id + "/read").header("Authorization", bearer(userToken)))
                .andExpect(jsonPath("$.read").value(true));
        mvc.perform(get("/api/v1/notifications/me/unread-count").header("Authorization", bearer(userToken)))
                .andExpect(jsonPath("$.unread").value(0));
    }

    @Test
    void onlyAdminsSendTestsAndReadTheDeliveryLog() throws Exception {
        mvc.perform(post("/api/v1/admin/notifications/test/" + userId).header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/notifications/log").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }

    @Test
    void adminTestShowsUpInTheCommandCentreFeed() throws Exception {
        mvc.perform(post("/api/v1/admin/notifications/test/" + userId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isAccepted());

        mvc.perform(get("/api/v1/admin/feed").header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("TEST"))
                .andExpect(jsonPath("$[0].summary").value("Test notification sent to Dev Sharma"));
        mvc.perform(get("/api/v1/admin/feed").header("Authorization", bearer(userToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void streamAcceptsATokenInTheQueryString() throws Exception {
        mvc.perform(get("/api/v1/stream").param("access_token", userToken))
                .andExpect(request().asyncStarted());
    }

    @Test
    void otherEndpointsIgnoreQueryStringTokens() throws Exception {
        mvc.perform(get("/api/v1/notifications/me").param("access_token", userToken))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/stream")).andExpect(status().isUnauthorized());
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static String unique() {
        return "c-" + UUID.randomUUID().toString().substring(0, 8) + "@campus.test";
    }
}
