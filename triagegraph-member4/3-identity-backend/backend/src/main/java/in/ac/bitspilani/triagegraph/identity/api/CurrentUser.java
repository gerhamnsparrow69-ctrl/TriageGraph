package in.ac.bitspilani.triagegraph.identity.api;

import org.springframework.security.oauth2.jwt.Jwt;

/** Reads the caller's user id from the token subject. */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static long id(Jwt jwt) {
        return Long.parseLong(jwt.getSubject());
    }
}
