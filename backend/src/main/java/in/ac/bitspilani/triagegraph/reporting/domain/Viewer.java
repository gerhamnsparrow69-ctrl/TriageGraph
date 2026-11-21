package in.ac.bitspilani.triagegraph.reporting.domain;

import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

/** Who is searching. A team lead also sees the incidents of the technicians in their team. */
public record Viewer(long userId, Role role, Set<Long> teamMemberIds) {

    public Viewer {
        teamMemberIds = teamMemberIds == null ? Set.of() : Set.copyOf(teamMemberIds);
    }

    public static Viewer of(long userId, Role role) {
        return new Viewer(userId, role, Set.of());
    }
}
