package in.ac.bitspilani.triagegraph.identity.application;

import java.util.Set;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;

/** Everything needed to create any kind of user. Technician fields are ignored for other roles. */
public record NewUser(Role role, String name, String email, String rawPassword, String department,
        Set<String> skills, int experienceYears, int maxLoad) {

    public static NewUser employee(String name, String email, String rawPassword, String department) {
        return new NewUser(Role.EMPLOYEE, name, email, rawPassword, department, Set.of(), 0, 0);
    }
}
