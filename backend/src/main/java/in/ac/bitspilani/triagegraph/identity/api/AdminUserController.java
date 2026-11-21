package in.ac.bitspilani.triagegraph.identity.api;

import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.identity.api.AuthDtos.CreateUserRequest;
import in.ac.bitspilani.triagegraph.identity.api.AuthDtos.UpdateTechnicianRequest;
import in.ac.bitspilani.triagegraph.identity.api.AuthDtos.UpdateUserRequest;
import in.ac.bitspilani.triagegraph.identity.api.AuthDtos.UserResponse;
import in.ac.bitspilani.triagegraph.identity.application.NewUser;
import in.ac.bitspilani.triagegraph.identity.application.UserAdminService;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin: users")
public class AdminUserController {

    private final UserAdminService admin;

    public AdminUserController(UserAdminService admin) {
        this.admin = admin;
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER')")
    public List<UserResponse> users() {
        return admin.all().stream().map(UserResponse::from).toList();
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse create(@Valid @RequestBody CreateUserRequest req) {
        var newUser = new NewUser(req.role(), req.name(), req.email(), req.password(), req.department(),
                req.skills() == null ? Set.of() : req.skills(), req.experienceYears(), req.maxLoad());
        return UserResponse.from(admin.create(newUser));
    }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse update(@PathVariable long id, @Valid @RequestBody UpdateUserRequest req) {
        return UserResponse.from(admin.updateProfile(id, req.name(), req.department(), req.pushTopic()));
    }

    @PostMapping("/users/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse deactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        if (CurrentUser.id(jwt) == id) {
            throw new InvalidInputException("You can't deactivate your own account.");
        }
        return UserResponse.from(admin.setActive(id, false));
    }

    @PostMapping("/users/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse activate(@PathVariable long id) {
        return UserResponse.from(admin.setActive(id, true));
    }

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('ADMIN', 'IT_MANAGER', 'TEAM_LEAD')")
    public List<UserResponse> technicians() {
        return admin.technicians().stream().map(UserResponse::from).toList();
    }

    @PutMapping("/technicians/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateTechnician(@PathVariable long id, @Valid @RequestBody UpdateTechnicianRequest req) {
        return UserResponse.from(admin.updateTechnician(id, req.skills(), req.experienceYears(), req.maxLoad(),
                req.available(), req.teamLeadId()));
    }
}
