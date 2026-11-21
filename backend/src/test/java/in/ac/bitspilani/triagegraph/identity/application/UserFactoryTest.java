package in.ac.bitspilani.triagegraph.identity.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import in.ac.bitspilani.triagegraph.identity.domain.TeamLead;
import in.ac.bitspilani.triagegraph.identity.domain.Technician;
import in.ac.bitspilani.triagegraph.identity.domain.User;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;

class UserFactoryTest {

    private static final Instant NOW = Instant.parse("2026-11-21T09:00:00Z");

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserFactory factory = new UserFactory(encoder, new PasswordPolicy(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @ParameterizedTest
    @EnumSource(Role.class)
    void createsTheRightSubclassForEveryRole(Role role) {
        User u = factory.create(new NewUser(role, "A User", "a@campus.test", "Secret123", null, Set.of("NETWORK"), 3, 5));
        assertThat(u.role()).isEqualTo(role);
        assertThat(u.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void teamLeadIsAlsoATechnician() {
        User u = factory.create(new NewUser(Role.TEAM_LEAD, "Lead", "l@campus.test", "Secret123", null, Set.of("CLOUD"), 9, 4));
        assertThat(u).isInstanceOf(TeamLead.class).isInstanceOf(Technician.class);
    }

    @Test
    void passwordIsHashedNeverStoredPlain() {
        User u = factory.create(NewUser.employee("Dev", "dev@campus.test", "Secret123", null));
        assertThat(u.getPasswordHash()).isNotEqualTo("Secret123");
        assertThat(encoder.matches("Secret123", u.getPasswordHash())).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"short1", "allletters", "1234567890", ""})
    void weakPasswordsAreRejected(String weak) {
        assertThatThrownBy(() -> factory.create(NewUser.employee("Dev", "dev@campus.test", weak, null)))
                .isInstanceOf(InvalidInputException.class);
    }
}
