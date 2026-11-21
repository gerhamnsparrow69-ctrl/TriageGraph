package in.ac.bitspilani.triagegraph.identity.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.identity.api.AuthDtos.LoginRequest;
import in.ac.bitspilani.triagegraph.identity.api.AuthDtos.LoginResponse;
import in.ac.bitspilani.triagegraph.identity.api.AuthDtos.RegisterRequest;
import in.ac.bitspilani.triagegraph.identity.api.AuthDtos.TokenResponse;
import in.ac.bitspilani.triagegraph.identity.api.AuthDtos.UserResponse;
import in.ac.bitspilani.triagegraph.identity.application.AuthService;
import in.ac.bitspilani.triagegraph.identity.application.IssuedToken;
import in.ac.bitspilani.triagegraph.identity.application.JwtService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    private final AuthService auth;
    private final JwtService jwt;

    public AuthController(AuthService auth, JwtService jwt) {
        this.auth = auth;
        this.jwt = jwt;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest req) {
        return UserResponse.from(auth.register(req.name(), req.email(), req.password(), req.department()));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        var result = auth.login(req.email(), req.password());
        return new LoginResponse(result.token().token(), result.token().expiresAt(), UserResponse.from(result.user()));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt principal) {
        return UserResponse.from(auth.current(CurrentUser.id(principal)));
    }

    /** Short-lived token for the notification stream (EventSource can't send an Authorization header). */
    @PostMapping("/stream-token")
    public TokenResponse streamToken(@AuthenticationPrincipal Jwt principal) {
        IssuedToken t = jwt.issueStreamToken(auth.current(CurrentUser.id(principal)));
        return new TokenResponse(t.token(), t.expiresAt());
    }
}
