package in.ac.bitspilani.triagegraph.incident.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

import java.util.Arrays;

/**
 * Who is acting on an incident. {@code id} and {@code role} are null for system actions
 * (auto-assignment, SLA monitor, correlation), which events and audit record as "system".
 */
public record Actor(Long id, Role role) {

    private static final Actor SYSTEM = new Actor(null, null);

    public static Actor system() {
        return SYSTEM;
    }

    public static Actor of(long id, Role role) {
        return new Actor(id, role);
    }

    public boolean isSystem() {
        return id == null;
    }

    public boolean hasAnyRole(Role... roles) {
        return role != null && Arrays.asList(roles).contains(role);
    }

    public boolean is(Long userId) {
        return id != null && id.equals(userId);
    }
}
