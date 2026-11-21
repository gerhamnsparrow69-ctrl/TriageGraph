package in.ac.bitspilani.triagegraph.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import in.ac.bitspilani.triagegraph.identity.application.NewUser;
import in.ac.bitspilani.triagegraph.identity.application.UserAdminService;
import in.ac.bitspilani.triagegraph.notification.application.NotificationService;
import in.ac.bitspilani.triagegraph.notification.application.Sleeper;
import in.ac.bitspilani.triagegraph.notification.domain.DeliveryStatus;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationLog;
import in.ac.bitspilani.triagegraph.notification.infrastructure.NtfyGateway;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.IncidentSnapshot;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.ChannelType;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.IncidentStatus;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.events.IncidentAssigned;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.IncidentQueryPort;

/** End to end through Spring: event published → dispatcher → real channels with fake SMTP and ntfy. */
@SpringBootTest
@ActiveProfiles("test")
class NotificationFlowTest {

    static final Instant T = Instant.parse("2026-11-21T10:00:00Z");

    /** Stands in for Member 1's incident module until it lands. */
    static volatile IncidentSnapshot incident;

    @TestConfiguration
    static class Fakes {

        @Bean
        IncidentQueryPort incidents() {
            return new IncidentQueryPort() {
                @Override
                public Optional<IncidentSnapshot> find(long id) {
                    return Optional.ofNullable(incident).filter(i -> i.id() == id);
                }

                @Override
                public List<IncidentSnapshot> openSince(Instant since) {
                    return incident == null ? List.of() : List.of(incident);
                }

                @Override
                public List<IncidentSnapshot> openWithDeadlineBefore(Instant t) {
                    return List.of();
                }
            };
        }

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
    ApplicationEventPublisher events;

    @Autowired
    UserAdminService admin;

    @Autowired
    NotificationService notifications;

    long reporterId;
    long technicianId;

    @BeforeEach
    void setUp() {
        reset(mail, ntfy);
        reporterId = admin.create(NewUser.employee("Dev Sharma", unique(), "Secret123", "Finance")).getId();
        technicianId = admin.create(new NewUser(Role.TECHNICIAN, "Sneha Reddy", unique(), "Secret123", null,
                Set.of("APPLICATION"), 4, 6)).getId();
        admin.updateProfile(technicianId, "Sneha Reddy", null, "tg-sneha-" + technicianId);
    }

    /** TC06: P1 assignment → in-app row, one email, one push at max priority. */
    @Test
    void p1AssignmentReachesTheTechnicianOnAllThreeChannels() {
        long incidentId = 1000 + technicianId;
        incident = snapshot(incidentId, Priority.P1);

        events.publishEvent(new IncidentAssigned(incidentId, "INC-" + incidentId, null, T, null, technicianId, "SMART"));

        await().atMost(Duration.ofSeconds(5)).until(() -> notifications.unread(technicianId) == 1);
        assertThat(notifications.latest(technicianId).getFirst().getSubject()).isEqualTo("[P1] INC-" + incidentId + " assigned to you");
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            verify(mail, times(1)).send(any(SimpleMailMessage.class));
            verify(ntfy, times(1)).publish(eq("tg-sneha-" + technicianId), anyString(), anyString(), eq(5));
        });
        assertThat(notifications.unread(reporterId)).as("reporter gets the bell only").isEqualTo(1);
    }

    /** TC20: SMTP down → 3 attempts, logged FAILED, other channels and the flow unaffected. */
    @Test
    void smtpOutageIsRetriedLoggedAndContained() {
        long incidentId = 2000 + technicianId;
        incident = snapshot(incidentId, Priority.P1);
        doThrow(new MailSendException("Connection refused")).when(mail).send(any(SimpleMailMessage.class));

        events.publishEvent(new IncidentAssigned(incidentId, "INC-" + incidentId, null, T, null, technicianId, "SMART"));

        await().atMost(Duration.ofSeconds(5)).until(() -> failedEmailFor(incidentId).isPresent());
        verify(mail, times(3)).send(any(SimpleMailMessage.class));
        assertThat(failedEmailFor(incidentId).get().getError()).contains("Connection refused");
        assertThat(notifications.unread(technicianId)).isEqualTo(1);
        verify(ntfy, times(1)).publish(anyString(), anyString(), anyString(), eq(5));
    }

    @Test
    void p3AssignmentSkipsEmail() {
        long incidentId = 3000 + technicianId;
        incident = snapshot(incidentId, Priority.P3);

        events.publishEvent(new IncidentAssigned(incidentId, "INC-" + incidentId, null, T, null, technicianId, "SMART"));

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                verify(ntfy, times(1)).publish(anyString(), anyString(), anyString(), eq(3)));
        verify(mail, times(0)).send(any(SimpleMailMessage.class));
    }

    @Test
    void adminTestNotificationUsesEveryChannel() {
        notifications.sendTest(technicianId);
        assertThat(notifications.unread(technicianId)).isEqualTo(1);
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(sent.capture());
        assertThat(sent.getValue().getSubject()).isEqualTo("[TriageGraph] Test notification");
        verify(ntfy).publish(eq("tg-sneha-" + technicianId), eq("Test notification"), anyString(), eq(3));
    }

    private Optional<NotificationLog> failedEmailFor(long incidentId) {
        return notifications.deliveryLog().stream()
                .filter(l -> l.getChannel() == ChannelType.EMAIL && Long.valueOf(incidentId).equals(l.getIncidentId())
                        && l.getStatus() == DeliveryStatus.FAILED)
                .findFirst();
    }

    private IncidentSnapshot snapshot(long id, Priority p) {
        return new IncidentSnapshot(id, "INC-" + id, "Fee payment fails", "500 on checkout", IncidentStatus.ASSIGNED,
                p, 1, "APPLICATION", reporterId, Set.of(), technicianId, null, T, T, T);
    }

    private static String unique() {
        return "n-" + UUID.randomUUID().toString().substring(0, 8) + "@campus.test";
    }
}
