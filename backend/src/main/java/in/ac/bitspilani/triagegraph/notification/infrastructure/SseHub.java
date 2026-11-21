package in.ac.bitspilani.triagegraph.notification.infrastructure;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import in.ac.bitspilani.triagegraph.notification.application.LivePush;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

/** Keeps every open browser stream and pushes events to the right ones. */
@Component
public class SseHub implements LivePush {

    static final Duration TIMEOUT = Duration.ofMinutes(30);

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);

    private record Client(long userId, Role role, SseEmitter emitter) {
    }

    private final Map<Long, CopyOnWriteArrayList<Client>> clients = new ConcurrentHashMap<>();

    public SseEmitter connect(long userId, Role role) {
        SseEmitter emitter = new SseEmitter(TIMEOUT.toMillis());
        Client client = new Client(userId, role, emitter);
        clients.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(client);
        Runnable remove = () -> remove(client);
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        send(client, "ready", Map.of("userId", userId));
        return emitter;
    }

    @Override
    public void toUser(long userId, String eventName, Object payload) {
        clients.getOrDefault(userId, new CopyOnWriteArrayList<>()).forEach(c -> send(c, eventName, payload));
    }

    @Override
    public void toRoles(Set<Role> roles, String eventName, Object payload) {
        clients.values().forEach(list -> list.stream()
                .filter(c -> roles.contains(c.role()))
                .forEach(c -> send(c, eventName, payload)));
    }

    /** Proxies and load balancers drop idle connections, so ping every 25 seconds. */
    @Scheduled(fixedRate = 25_000)
    void heartbeat() {
        clients.values().forEach(list -> list.forEach(c -> {
            try {
                c.emitter().send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                remove(c);
            }
        }));
    }

    int connectionCount() {
        return clients.values().stream().mapToInt(CopyOnWriteArrayList::size).sum();
    }

    private void send(Client c, String eventName, Object payload) {
        try {
            c.emitter().send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException | IllegalStateException e) {
            log.debug("Dropping stream for user {}: {}", c.userId(), e.getMessage());
            remove(c);
        }
    }

    private void remove(Client c) {
        var list = clients.get(c.userId());
        if (list != null) {
            list.remove(c);
            if (list.isEmpty()) {
                clients.remove(c.userId(), list);
            }
        }
    }
}
