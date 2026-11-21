package in.ac.bitspilani.triagegraph.incident.api;

import in.ac.bitspilani.triagegraph.incident.domain.Actor;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Role;
import in.ac.bitspilani.triagegraph.shared.exception.UnauthorizedOperationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/** Turns the caller's token (subject = user id, "roles" claim) into an {@link Actor}. */
final class JwtActors {

    static final String ROLES_CLAIM = "roles";

    private JwtActors() {
    }

    static Actor from(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList(ROLES_CLAIM);
        if (jwt.getSubject() == null || roles == null || roles.isEmpty()) {
            throw new UnauthorizedOperationException("The token does not identify a user.");
        }
        try {
            return Actor.of(Long.parseLong(jwt.getSubject()), Role.valueOf(roles.getFirst()));
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedOperationException("The token does not identify a user.", e);
        }
    }
}
