package in.ac.bitspilani.triagegraph.seed;

import static in.ac.bitspilani.triagegraph.shared.contracts.enums.Role.ADMIN;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.Role.EMPLOYEE;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.Role.IT_MANAGER;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.Role.TEAM_LEAD;
import static in.ac.bitspilani.triagegraph.shared.contracts.enums.Role.TECHNICIAN;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.identity.application.NewUser;
import in.ac.bitspilani.triagegraph.identity.application.UserAdminService;
import in.ac.bitspilani.triagegraph.identity.domain.TeamLead;
import in.ac.bitspilani.triagegraph.identity.domain.Technician;
import in.ac.bitspilani.triagegraph.identity.domain.User;
import in.ac.bitspilani.triagegraph.identity.domain.UserRepository;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

/** Demo users from blueprint Appendix A. All share the password {@value #DEMO_PASSWORD}. */
@Component
@Profile({"dev", "demo"})
class UserSeeder implements DataSeeder {

    static final String DEMO_PASSWORD = "Demo@1234";
    static final String DOMAIN = "@triagegraph.local";

    private record Seed(String name, Role role, String department, Set<String> skills, int years, int maxLoad,
            String lead) {
    }

    private static final List<Seed> USERS = List.of(
            new Seed("Karthik Rao", TEAM_LEAD, "Infrastructure", Set.of("NETWORK", "CLOUD", "STORAGE"), 9, 4, null),
            new Seed("Ananya Iyer", TEAM_LEAD, "Applications & Desk", Set.of("APPLICATION", "DATABASE", "SOFTWARE"), 8, 4, null),
            new Seed("Ravi Kumar", TECHNICIAN, "Infrastructure", Set.of("NETWORK", "AUTHENTICATION"), 5, 6, "Karthik Rao"),
            new Seed("Sneha Reddy", TECHNICIAN, "Applications & Desk", Set.of("APPLICATION", "DATABASE"), 4, 6, "Ananya Iyer"),
            new Seed("Arjun Mehta", TECHNICIAN, "Applications & Desk", Set.of("HARDWARE", "SOFTWARE"), 2, 8, "Ananya Iyer"),
            new Seed("Priya Nair", TECHNICIAN, "Infrastructure", Set.of("SECURITY", "AUTHENTICATION", "CLOUD"), 6, 5, "Karthik Rao"),
            new Seed("Vikram Singh", ADMIN, "IT Services", Set.of(), 0, 0, null),
            new Seed("Dr Meera Joshi", IT_MANAGER, "IT Services", Set.of(), 0, 0, null),
            new Seed("Dev Sharma", EMPLOYEE, "Finance office", Set.of(), 0, 0, null),
            new Seed("Rahul Verma", EMPLOYEE, "Student, Hostel Block 5", Set.of(), 0, 0, null),
            new Seed("Fatima Khan", EMPLOYEE, "Faculty, CSIS department", Set.of(), 0, 0, null));

    private final UserRepository users;
    private final UserAdminService admin;

    UserSeeder(UserRepository users, UserAdminService admin) {
        this.users = users;
        this.admin = admin;
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    @Transactional
    public void seed() {
        Map<String, User> byName = new HashMap<>();
        for (Seed s : USERS) {
            String email = emailFor(s.name());
            User user = users.findByEmail(email).orElseGet(() -> admin.create(new NewUser(s.role(), s.name(), email,
                    DEMO_PASSWORD, s.department(), s.skills(), s.years(), s.maxLoad())));
            byName.put(s.name(), user);
        }
        for (Seed s : USERS) {
            if (s.lead() != null && byName.get(s.name()) instanceof Technician tech
                    && byName.get(s.lead()) instanceof TeamLead lead && tech.getTeamLead() == null) {
                tech.reportTo(lead);
            }
            User u = byName.get(s.name());
            if (u.getPushTopic() == null) {
                u.updateProfile(u.getName(), u.getDepartment(), "triagegraph-" + slug(s.name()));
            }
        }
    }

    static String emailFor(String name) {
        return slug(name).replace('-', '.') + DOMAIN;
    }

    private static String slug(String name) {
        return name.toLowerCase().replace("dr ", "").trim().replaceAll("[^a-z0-9]+", "-");
    }
}
