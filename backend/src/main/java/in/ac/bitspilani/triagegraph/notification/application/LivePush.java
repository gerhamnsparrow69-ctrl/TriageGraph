package in.ac.bitspilani.triagegraph.notification.application;

import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

/** Port for pushing live updates to open browsers (implemented over SSE). */
public interface LivePush {

    void toUser(long userId, String eventName, Object payload);

    void toRoles(Set<Role> roles, String eventName, Object payload);
}
