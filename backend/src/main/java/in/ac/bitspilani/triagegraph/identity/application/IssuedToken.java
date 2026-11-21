package in.ac.bitspilani.triagegraph.identity.application;

import java.time.Instant;

public record IssuedToken(String token, Instant expiresAt) {
}
