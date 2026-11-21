package in.ac.bitspilani.triagegraph.seed;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.identity.application.AuthService;
import in.ac.bitspilani.triagegraph.identity.domain.Technician;
import in.ac.bitspilani.triagegraph.identity.domain.UserRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;

@SpringBootTest
@ActiveProfiles({"test", "demo"})
@Transactional
class UserSeederTest {

    @Autowired
    UserSeeder seeder;

    @Autowired
    UserRepository users;

    @Autowired
    UserDirectory directory;

    @Autowired
    AuthService auth;

    @Test
    void seedsAppendixAUsersWithTeamsAndIsIdempotent() {
        seeder.seed(); // already ran at startup; a second run must not duplicate anyone
        assertThat(users.findAll()).hasSize(11);
        assertThat(directory.technicians()).hasSize(6);
        assertThat(directory.withRole(Role.EMPLOYEE)).hasSize(3);

        Technician sneha = (Technician) users.findByEmail("sneha.reddy@triagegraph.local").orElseThrow();
        assertThat(sneha.getTeamLead().getName()).isEqualTo("Ananya Iyer");
        assertThat(directory.teamLeadOf(sneha.getId())).get().extracting(r -> r.name()).isEqualTo("Ananya Iyer");
        assertThat(sneha.getPushTopic()).isEqualTo("triagegraph-sneha-reddy");
    }

    @Test
    void demoAccountsLogInWithTheDemoPassword() {
        var result = auth.login("meera.joshi@triagegraph.local", UserSeeder.DEMO_PASSWORD);
        assertThat(result.user().role()).isEqualTo(Role.IT_MANAGER);
        assertThat(result.token().token()).isNotBlank();
    }
}
