package in.ac.bitspilani.triagegraph.reporting.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

class ViewerTest {

    @Test
    void teamIsCopiedAndReadOnly() {
        Set<Long> team = new HashSet<>(Set.of(1L));
        Viewer v = new Viewer(9, Role.TEAM_LEAD, team);
        team.add(2L);
        assertThat(v.teamMemberIds()).containsExactly(1L);
        assertThatThrownBy(() -> v.teamMemberIds().add(3L)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(Viewer.of(9, Role.ADMIN).teamMemberIds()).isEmpty();
    }

    @Test
    void factsAndServicesAreSimpleValues() {
        assertThat(new TechnicianFact(1, "Ravi", "TECHNICIAN", 6, true, 2L).maxLoad()).isEqualTo(6);
        assertThat(new ServiceFact(1, "LMS", "HIGH").name()).isEqualTo("LMS");
    }
}
