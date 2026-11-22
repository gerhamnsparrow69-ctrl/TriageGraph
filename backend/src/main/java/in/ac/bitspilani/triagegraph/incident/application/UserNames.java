package in.ac.bitspilani.triagegraph.incident.application;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.UserRef;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.UserNotFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Display names for the people on one incident view (reporter, assignee, note authors). Resolved on the
 * server so every role sees names without calling the admin-only user endpoints. A user who cannot be
 * found (deleted, or a stale id) gets {@code null}; the UI shows a neutral fallback instead.
 * Not thread-safe; create one per view.
 */
final class UserNames {

    private final UserDirectory users;
    private final Map<Long, String> cache = new HashMap<>();

    UserNames(UserDirectory users) {
        this.users = users;
    }

    /** Null for a null id (system), or when the user is unknown. */
    String of(Long id) {
        if (id == null) {
            return null;
        }
        return cache.computeIfAbsent(id, this::lookup);
    }

    private String lookup(long id) {
        try {
            UserRef ref = users.user(id);
            return ref == null || ref.name() == null || ref.name().isBlank() ? null : ref.name();
        } catch (UserNotFoundException e) {
            return null;
        }
    }
}
