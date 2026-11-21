package in.ac.bitspilani.triagegraph.notification.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import in.ac.bitspilani.triagegraph.notification.application.FeedItem;
import in.ac.bitspilani.triagegraph.notification.application.LiveFeed;
import in.ac.bitspilani.triagegraph.notification.application.NotificationService;
import in.ac.bitspilani.triagegraph.notification.domain.NotificationLog;
import in.ac.bitspilani.triagegraph.notification.infrastructure.NotificationView;
import in.ac.bitspilani.triagegraph.notification.infrastructure.SseHub;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationService notifications;
    private final SseHub hub;
    private final LiveFeed feed;

    public NotificationController(NotificationService notifications, SseHub hub, LiveFeed feed) {
        this.notifications = notifications;
        this.hub = hub;
        this.feed = feed;
    }

    @GetMapping("/notifications/me")
    public List<NotificationView> mine(@AuthenticationPrincipal Jwt jwt) {
        return notifications.latest(userId(jwt)).stream().map(NotificationView::from).toList();
    }

    @GetMapping("/notifications/me/unread-count")
    public Map<String, Long> unreadCount(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("unread", notifications.unread(userId(jwt)));
    }

    @PostMapping("/notifications/{id}/read")
    public NotificationView markRead(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        return NotificationView.from(notifications.markRead(userId(jwt), id));
    }

    @PostMapping("/notifications/me/read-all")
    public Map<String, Integer> markAllRead(@AuthenticationPrincipal Jwt jwt) {
        return Map.of("marked", notifications.markAllRead(userId(jwt)));
    }

    @Operation(summary = "Live stream of notifications (and, for admins, the Command Centre feed)",
            description = "EventSource can't send headers: get a token from POST /auth/stream-token and pass it as ?access_token=")
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("roles");
        Role role = roles == null || roles.isEmpty() ? Role.EMPLOYEE : Role.valueOf(roles.getFirst());
        return hub.connect(userId(jwt), role);
    }

    @Operation(summary = "Recent Command Centre feed items, newest first; live ones arrive as 'feed' events on /stream")
    @GetMapping("/admin/feed")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER', 'TEAM_LEAD')")
    public List<FeedItem> recentFeed() {
        return feed.recent();
    }

    @GetMapping("/admin/notifications/log")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER')")
    public List<DeliveryLogView> deliveryLog() {
        return notifications.deliveryLog().stream().map(DeliveryLogView::from).toList();
    }

    @PostMapping("/admin/notifications/test/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void sendTest(@PathVariable long userId) {
        notifications.sendTest(userId);
    }

    private static long userId(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }

    public record DeliveryLogView(long id, long recipientId, String channel, String eventType, Long incidentId,
            String status, String error, java.time.Instant at) {

        static DeliveryLogView from(NotificationLog l) {
            return new DeliveryLogView(l.getId(), l.getRecipientId(), l.getChannel().name(), l.getEventType(),
                    l.getIncidentId(), l.getStatus().name(), l.getError(), l.getCreatedAt());
        }
    }
}
