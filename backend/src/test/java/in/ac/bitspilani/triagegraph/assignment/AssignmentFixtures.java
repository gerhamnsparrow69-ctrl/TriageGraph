package in.ac.bitspilani.triagegraph.assignment;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.ObjectProvider;

import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentContext;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.AssignmentRequest;
import in.ac.bitspilani.triagegraph.shared.contracts.dto.TechnicianProfile;
import in.ac.bitspilani.triagegraph.shared.contracts.enums.Priority;

/** Test data shared by the assignment tests: the seeded campus technicians from Appendix A. */
public final class AssignmentFixtures {

    public static final long FEE_PAYMENT_APP = 1L;
    public static final long DATABASE_CLUSTER = 8L;

    private AssignmentFixtures() {
    }

    public static TechnicianProfile tech(long id, String name, Set<String> skills, int load, int max, int years) {
        return new TechnicianProfile(id, name, skills, load, max, years, Map.of(), true);
    }

    public static TechnicianProfile sneha(int load) {
        return tech(4, "Sneha Reddy", Set.of("APPLICATION", "DATABASE"), load, 6, 4);
    }

    public static TechnicianProfile ananya(int load) {
        return tech(2, "Ananya Iyer", Set.of("APPLICATION", "DATABASE", "SOFTWARE"), load, 4, 8);
    }

    public static TechnicianProfile arjun(int load) {
        return tech(5, "Arjun Mehta", Set.of("HARDWARE", "SOFTWARE"), load, 8, 2);
    }

    public static TechnicianProfile ravi(int load) {
        return tech(3, "Ravi Kumar", Set.of("NETWORK", "AUTHENTICATION"), load, 6, 5);
    }

    public static TechnicianProfile withFixes(TechnicianProfile p, Map<Long, Integer> fixes) {
        return new TechnicianProfile(p.id(), p.name(), p.skills(), p.activeLoad(), p.maxLoad(), p.experienceYears(),
                fixes, p.available());
    }

    public static TechnicianProfile unavailable(TechnicianProfile p) {
        return new TechnicianProfile(p.id(), p.name(), p.skills(), p.activeLoad(), p.maxLoad(), p.experienceYears(),
                p.fixesByService(), false);
    }

    public static AssignmentRequest request(String category, long serviceId, Priority priority) {
        return new AssignmentRequest(1001L, category, serviceId, priority);
    }

    public static AssignmentContext context(String category, long serviceId, Priority priority) {
        return new AssignmentContext(request(category, serviceId, priority), category);
    }

    /** An ObjectProvider that returns a fixed value (null = bean not deployed). */
    public static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                if (value == null) {
                    throw new IllegalStateException("not available");
                }
                return value;
            }

            @Override
            public T getObject(Object... args) {
                return getObject();
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }
        };
    }
}
