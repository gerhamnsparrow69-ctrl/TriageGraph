package in.ac.bitspilani.triagegraph.config;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Settings under {@code app.security.*}.
 *
 * @param jwtSecret      HS256 signing key; at least 32 characters (256 bits)
 * @param jwtTtl         how long an issued access token stays valid
 * @param allowedOrigins browser origins allowed by CORS (the Vite dev server by default)
 */
@Validated
@ConfigurationProperties(prefix = "app.security")
public record SecurityProperties(
        @NotBlank @Size(min = 32, message = "JWT_SECRET must be at least 32 characters for HS256") String jwtSecret,
        @NotNull Duration jwtTtl,
        @NotNull List<String> allowedOrigins) {
}
