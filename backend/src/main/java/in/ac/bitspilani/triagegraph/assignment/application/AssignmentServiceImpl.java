package in.ac.bitspilani.triagegraph.assignment.application;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentContext;
import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentStrategy;
import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentStrategyRegistry;
import in.ac.bitspilani.triagegraph.assignment.domain.EligibilityFilter;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.AssignmentRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianRecommendation;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.AssignmentService;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.UserDirectory;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.ItsmException;

/**
 * Implements the {@link AssignmentService} port that Member 1's lifecycle calls. Every request follows the
 * same pipeline: resolve the required skill, load technicians (with their fix history), filter eligible
 * ones, then let the active strategy rank them.
 */
@Service
public class AssignmentServiceImpl implements AssignmentService {

    /** What each strategy would choose for the same incident (the admin's "compare strategies" tool). */
    public record StrategyOutcome(String strategyKey, String displayName, boolean active,
            TechnicianRecommendation pick, String error) {
    }

    private final UserDirectory directory;
    private final AssignmentStrategyRegistry registry;
    private final AssignmentSettingsService settings;
    private final ExperienceService experience;
    private final RequiredSkillResolver skills;
    private final EligibilityFilter filter = new EligibilityFilter();

    public AssignmentServiceImpl(UserDirectory directory, AssignmentStrategyRegistry registry,
            AssignmentSettingsService settings, ExperienceService experience, RequiredSkillResolver skills) {
        this.directory = directory;
        this.registry = registry;
        this.settings = settings;
        this.experience = experience;
        this.skills = skills;
    }

    @Override
    public TechnicianRecommendation autoAssign(AssignmentRequest req) {
        AssignmentStrategy strategy = settings.activeStrategy();
        AssignmentContext context = contextFor(req);
        TechnicianRecommendation chosen = rank(strategy, context).get(0);
        strategy.onAssigned(context, chosen.technicianId());
        return chosen;
    }

    @Override
    public List<TechnicianRecommendation> recommend(AssignmentRequest req, int topK) {
        return recommendWith(settings.activeStrategy(), req, topK);
    }

    /** @throws InvalidInputException unknown strategy key */
    public List<TechnicianRecommendation> recommendWith(String strategyKey, AssignmentRequest req, int topK) {
        return recommendWith(AssignmentSettingsService.requireKnown(registry, strategyKey), req, topK);
    }

    public List<StrategyOutcome> compare(AssignmentRequest req) {
        String active = settings.current().getActiveStrategy();
        AssignmentContext context = contextFor(req);
        List<StrategyOutcome> outcomes = new ArrayList<>();
        for (AssignmentStrategy s : registry.all()) {
            boolean isActive = s.key().equalsIgnoreCase(active);
            try {
                outcomes.add(new StrategyOutcome(s.key(), s.displayName(), isActive, rank(s, context).get(0), null));
            } catch (ItsmException e) {
                outcomes.add(new StrategyOutcome(s.key(), s.displayName(), isActive, null, e.getMessage()));
            }
        }
        return outcomes;
    }

    private List<TechnicianRecommendation> recommendWith(AssignmentStrategy strategy, AssignmentRequest req, int topK) {
        List<TechnicianRecommendation> ranked = rank(strategy, contextFor(req));
        return ranked.subList(0, Math.min(Math.max(1, topK), ranked.size()));
    }

    private AssignmentContext contextFor(AssignmentRequest req) {
        if (req == null) {
            throw new InvalidInputException("An assignment request is required.");
        }
        if (req.priority() == null) {
            throw new InvalidInputException("The incident needs a priority before a technician can be chosen.");
        }
        return new AssignmentContext(req, skills.requiredSkillFor(req.categoryCode()));
    }

    private List<TechnicianRecommendation> rank(AssignmentStrategy strategy, AssignmentContext context) {
        List<TechnicianProfile> technicians = experience.enrich(directory.technicians());
        List<TechnicianProfile> eligible = filter.eligible(context.requiredSkill(), technicians);
        return strategy.rank(context, eligible);
    }
}
