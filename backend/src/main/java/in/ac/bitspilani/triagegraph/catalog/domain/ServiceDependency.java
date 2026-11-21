package in.ac.bitspilani.triagegraph.catalog.domain;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(
    name = "service_dependencies",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"dependent_service_id", "dependency_service_id"}
    )
)
public class ServiceDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependent_service_id", nullable = false)
    private ITService dependent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dependency_service_id", nullable = false)
    private ITService dependency;

    protected ServiceDependency() {
    }

    public ServiceDependency(ITService dependent, ITService dependency) {
        this.dependent = Objects.requireNonNull(dependent);
        this.dependency = Objects.requireNonNull(dependency);

        if (dependent.equals(dependency)) {
            throw new IllegalArgumentException(
                "A service cannot depend on itself"
            );
        }
    }

    public Long getId() {
        return id;
    }

    public ITService getDependent() {
        return dependent;
    }

    public ITService getDependency() {
        return dependency;
    }
}