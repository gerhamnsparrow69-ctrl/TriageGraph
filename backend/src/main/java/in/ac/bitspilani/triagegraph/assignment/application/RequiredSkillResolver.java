package in.ac.bitspilani.triagegraph.assignment.application;

import java.util.Locale;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import in.ac.bitspilani.triagegraph.shared.contracts.dto.CategoryRef;
import in.ac.bitspilani.triagegraph.shared.contracts.ports.CatalogQuery;
import in.ac.bitspilani.triagegraph.shared.exception.InvalidInputException;
import in.ac.bitspilani.triagegraph.shared.exception.ItsmException;

/**
 * Which skill an incident category needs. Member 3's catalog can map a category to a different default
 * skill; without a catalog (or without a mapping) the category code itself is the skill, which matches the
 * seeded technicians (skills APPLICATION, NETWORK, ...).
 */
@Component
public class RequiredSkillResolver {

    private final ObjectProvider<CatalogQuery> catalog;

    public RequiredSkillResolver(ObjectProvider<CatalogQuery> catalog) {
        this.catalog = catalog;
    }

    public String requiredSkillFor(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            throw new InvalidInputException("An incident category is needed to choose a technician.");
        }
        String code = categoryCode.trim().toUpperCase(Locale.ROOT);
        CatalogQuery query = catalog.getIfAvailable();
        if (query != null) {
            try {
                CategoryRef ref = query.category(code);
                if (ref != null && ref.defaultSkill() != null && !ref.defaultSkill().isBlank()) {
                    return ref.defaultSkill().trim().toUpperCase(Locale.ROOT);
                }
            } catch (ItsmException unknownCategory) {
                // fall through: use the code itself
            }
        }
        return code;
    }
}
