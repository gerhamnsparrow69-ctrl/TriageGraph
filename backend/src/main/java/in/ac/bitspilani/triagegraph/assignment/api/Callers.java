package in.ac.bitspilani.triagegraph.assignment.api;

import org.springframework.security.oauth2.jwt.Jwt;

import in.ac.bitspilani.triagegraph.shared.exception.AuthenticationFailedException;

/** The caller's user id is the token subject (set by Member 4's JwtService). */
final class Callers {

    private Callers() {
    }

    static long id(Jwt jwt) {
        if (jwt == null || jwt.getSubject() == null) {
            throw new AuthenticationFailedException("Not signed in.");
        }
        return Long.parseLong(jwt.getSubject());
    }
}
