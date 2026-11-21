package in.ac.bitspilani.triagegraph.notification.application;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Settings under {@code app.notify.*}. */
@Validated
@ConfigurationProperties(prefix = "app.notify")
public record NotifyProperties(
        @NotBlank String mailFrom,
        @NotBlank String ntfyBaseUrl,
        boolean emailEnabled,
        boolean pushEnabled,
        @Min(1) int retryAttempts,
        @NotNull Duration retryBackoff) {
}
