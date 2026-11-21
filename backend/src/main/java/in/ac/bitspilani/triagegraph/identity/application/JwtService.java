package in.ac.bitspilani.triagegraph.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.config.SecurityProperties;
import in.ac.bitspilani.triagegraph.identity.domain.User;

/** Issues HS256 tokens: subject = user id, plus email, name and roles claims. */
@Service
public class JwtService {

    public static final String ISSUER = "triagegraph";

    private final JwtEncoder encoder;
    private final Clock clock;
    private final Duration ttl;

    public JwtService(JwtEncoder encoder, Clock clock, SecurityProperties props) {
        this.encoder = encoder;
        this.clock = clock;
        this.ttl = props.jwtTtl();
    }

    public IssuedToken issue(User user) {
        return issue(user, ttl);
    }

    /** Short-lived token for the SSE stream, which has to pass it as a query parameter. */
    public IssuedToken issueStreamToken(User user) {
        return issue(user, Duration.ofMinutes(5));
    }

    private IssuedToken issue(User user, Duration lifetime) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plus(lifetime);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(expiresAt)
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("roles", List.of(user.role().name()))
                .build();
        Jwt jwt = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
        return new IssuedToken(jwt.getTokenValue(), expiresAt);
    }
}
