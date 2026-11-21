package in.ac.bitspilani.triagegraph.catalog.domain;

import in.ac.bitspilani.triagegraph.shared.contracts.enums.Criticality;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
@Table(name = "it_services")
public class ITService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Criticality criticality;

    protected ITService() {
    }

    public ITService(String code, String name, Criticality criticality) {
        this.code = Objects.requireNonNull(code);
        this.name = Objects.requireNonNull(name);
        this.criticality = Objects.requireNonNull(criticality);
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Criticality getCriticality() {
        return criticality;
    }
}