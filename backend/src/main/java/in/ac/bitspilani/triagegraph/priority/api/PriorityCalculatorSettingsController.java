package in.ac.bitspilani.triagegraph.priority.api;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import in.ac.bitspilani.triagegraph.priority.application.PriorityCalculatorRegistry;

/** Admin view of which priority calculator is active and which ones exist. */
@RestController
@RequestMapping("/api/v1/admin/settings/priority-calculator")
public class PriorityCalculatorSettingsController {

    public record CalculatorOption(String key, String description) {}

    public record PriorityCalculatorSetting(String activeKey, List<CalculatorOption> available) {}

    private final PriorityCalculatorRegistry registry;

    public PriorityCalculatorSettingsController(PriorityCalculatorRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PriorityCalculatorSetting current() {
        List<CalculatorOption> options = registry.all().stream()
                .map(c -> new CalculatorOption(c.key(), c.description()))
                .toList();
        return new PriorityCalculatorSetting(registry.activeKey(), options);
    }
}
