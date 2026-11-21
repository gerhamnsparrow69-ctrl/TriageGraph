package in.ac.bitspilani.triagegraph.shared;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import in.ac.bitspilani.triagegraph.audit.domain.AuditRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.repository.Repository;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.Arrays;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * TC32 (architecture part): the blueprint §3 rules, checked on the production classes of every module.
 * The audit part of TC32 (one tampered row is reported by id) is in AuditTrailIntegrationTest.
 */
@AnalyzeClasses(packages = "in.ac.bitspilani.triagegraph", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDoesNotDependOnInfrastructureOrApi = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..infrastructure..", "..api..")
            .as("§3: ..domain.. must not depend on ..infrastructure.. or ..api..");

    @ArchTest
    static final ArchRule controllersDoNotUseRepositories = noClasses()
            .that().areAnnotatedWith(RestController.class)
            .should().dependOnClassesThat().areAssignableTo(Repository.class)
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
            .as("§3: controllers go through application services, never repositories");

    @ArchTest
    static final ArchRule modulesAreFreeOfCycles = slices()
            .matching("in.ac.bitspilani.triagegraph.(*)..")
            .should().beFreeOfCycles()
            .as("§3: feature modules must be free of dependency cycles");

    @ArchTest
    static final ArchRule externalLibrariesOnlyInInfrastructure = noClasses()
            .that().resideOutsideOfPackage("..infrastructure..")
            .should().dependOnClassesThat().resideInAnyPackage("dev.langchain4j..", "org.jgrapht..",
                    "com.google.genai..", "com.google.cloud.vertexai..")
            .as("§3: only ..infrastructure.. may use LangChain4j, JGraphT or the Gemini client");

    @ArchTest
    static final ArchRule incidentDomainDoesNotKnowOtherModules = noClasses()
            .that().resideInAPackage("..incident.domain..")
            .should().dependOnClassesThat().resideInAnyPackage("..identity..", "..triage..", "..assignment..",
                    "..catalog..", "..topology..", "..sla..", "..notification..", "..audit..")
            .as("modules talk through shared contracts: the incident aggregate holds ids, not other modules' entities");

    /** Interface Segregation (§10.2): nothing can rewrite audit history through the repository. */
    @Test
    void auditRepositoryHasNoUpdateOrDelete() {
        assertThat(Arrays.stream(AuditRepository.class.getMethods()).map(Method::getName))
                .noneMatch(n -> n.startsWith("delete") || n.startsWith("update") || n.startsWith("remove"))
                .contains("append");
    }
}
