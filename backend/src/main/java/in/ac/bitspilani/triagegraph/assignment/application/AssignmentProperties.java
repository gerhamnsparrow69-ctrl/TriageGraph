package in.ac.bitspilani.triagegraph.assignment.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code app.assignment.*}.
 *
 * @param defaultStrategy strategy used until an admin picks one (SMART, SKILL_BASED, LOAD_BALANCED, ROUND_ROBIN)
 * @param experienceCap   past fixes on a service at which the smart score's experience term is full
 */
@ConfigurationProperties(prefix = "app.assignment")
public record AssignmentProperties(String defaultStrategy, Integer experienceCap) {

    public AssignmentProperties {
        if (defaultStrategy == null || defaultStrategy.isBlank()) {
            defaultStrategy = "SMART";
        }
        if (experienceCap == null || experienceCap < 1) {
            experienceCap = 5;
        }
    }
}
