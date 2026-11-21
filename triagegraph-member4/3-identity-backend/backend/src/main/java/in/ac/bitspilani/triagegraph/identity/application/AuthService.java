package in.ac.bitspilani.triagegraph.identity.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.ac.bitspilani.triagegraph.identity.domain.User;
import in.ac.bitspilani.triagegraph.identity.domain.UserRepository;
import in.ac.bitspilani.triagegraph.shared.exception.AuthenticationFailedException;
import in.ac.bitspilani.triagegraph.shared.exception.UserNotFoundException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /** Same message for unknown email and wrong password, so attackers can't probe for accounts. */
    static final String BAD_CREDENTIALS = "Invalid email or password.";

    private final UserRepository users;
    private final UserFactory factory;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Clock clock;

    public AuthService(UserRepository users, UserFactory factory, PasswordEncoder encoder, JwtService jwt, Clock clock) {
        this.users = users;
        this.factory = factory;
        this.encoder = encoder;
        this.jwt = jwt;
        this.clock = clock;
    }

    /** Self-registration always creates an Employee; staff accounts come from an admin. */
    @Transactional
    public User register(String name, String email, String password, String department) {
        String normalised = User.normaliseEmail(email);
        if (users.existsByEmail(normalised)) {
            throw new EmailAlreadyUsedException(normalised);
        }
        return users.save(factory.create(NewUser.employee(name, normalised, password, department)));
    }

    /** Failed attempts must be saved even though we throw, hence noRollbackFor. */
    @Transactional(noRollbackFor = AuthenticationFailedException.class)
    public LoginResult login(String email, String password) {
        Instant now = Instant.now(clock);
        Optional<User> found = users.findByEmail(User.normaliseEmail(email));
        if (found.isEmpty() || !found.get().isActive()) {
            log.info("LOGIN_FAILED unknown or inactive account");
            throw new AuthenticationFailedException(BAD_CREDENTIALS);
        }
        User user = found.get();
        if (user.isLocked(now)) {
            throw new AccountLockedException(user.getLockedUntil());
        }
        if (!encoder.matches(password, user.getPasswordHash())) {
            user.recordFailedLogin(now);
            log.info("LOGIN_FAILED userId={}", user.getId());
            throw new AuthenticationFailedException(BAD_CREDENTIALS);
        }
        user.recordSuccessfulLogin();
        return new LoginResult(jwt.issue(user), user);
    }

    @Transactional(readOnly = true)
    public User current(long userId) {
        return users.findById(userId).orElseThrow(() -> new UserNotFoundException("No user with id " + userId));
    }

    public record LoginResult(IssuedToken token, User user) {
    }
}
