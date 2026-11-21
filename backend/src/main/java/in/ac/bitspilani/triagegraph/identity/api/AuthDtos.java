package in.ac.bitspilani.triagegraph.identity.api;

import java.time.Instant;
import java.util.Set;

import in.ac.bitspilani.triagegraph.identity.domain.Technician;
import in.ac.bitspilani.triagegraph.identity.domain.User;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request and response bodies for the identity endpoints. Passwords never appear in a response. */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password,
            @Size(max = 100) String department) {
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {
    }

    public record LoginResponse(String token, Instant expiresAt, UserResponse user) {
    }

    public record TokenResponse(String token, Instant expiresAt) {
    }

    public record UserResponse(long id, String name, String email, Role role, String department,
            String pushTopic, boolean active, TechnicianDetails technician) {

        public static UserResponse from(User u) {
            TechnicianDetails tech = u instanceof Technician t ? TechnicianDetails.from(t) : null;
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.role(), u.getDepartment(),
                    u.getPushTopic(), u.isActive(), tech);
        }
    }

    public record TechnicianDetails(Set<String> skills, int experienceYears, int maxLoad, boolean available,
            Long teamLeadId, String teamLeadName) {

        static TechnicianDetails from(Technician t) {
            var lead = t.getTeamLead();
            return new TechnicianDetails(t.getSkills(), t.getExperienceYears(), t.getMaxLoad(), t.isAvailable(),
                    lead == null ? null : lead.getId(), lead == null ? null : lead.getName());
        }
    }

    public record CreateUserRequest(
            @NotNull Role role,
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Size(max = 72) String password,
            @Size(max = 100) String department,
            Set<@NotBlank @Size(max = 40) String> skills,
            @Min(0) @Max(60) int experienceYears,
            @Min(0) @Max(50) int maxLoad) {
    }

    public record UpdateUserRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 100) String department,
            @Size(max = 100) String pushTopic) {
    }

    public record UpdateTechnicianRequest(
            @NotNull Set<@NotBlank @Size(max = 40) String> skills,
            @Min(0) @Max(60) int experienceYears,
            @Min(1) @Max(50) int maxLoad,
            boolean available,
            Long teamLeadId) {
    }
}
