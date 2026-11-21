package in.ac.bitspilani.triagegraph.catalog.domain;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @Column(length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String defaultSkill;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection
    @CollectionTable(
        name = "category_keywords",
        joinColumns = @JoinColumn(name = "category_code")
    )
    @Column(name = "keyword", nullable = false)
    private List<String> keywords = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
        name = "category_aliases",
        joinColumns = @JoinColumn(name = "category_code")
    )
    @Column(name = "alias", nullable = false)
    private List<String> aliases = new ArrayList<>();

    protected Category() {
    }

    public Category(String code, String name, String defaultSkill,
                    List<String> keywords, List<String> aliases) {
        this.code = Objects.requireNonNull(code);
        this.name = Objects.requireNonNull(name);
        this.defaultSkill = defaultSkill;
        this.keywords = new ArrayList<>(keywords);
        this.aliases = new ArrayList<>(aliases);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDefaultSkill() {
        return defaultSkill;
    }

    public List<String> getKeywords() {
        return List.copyOf(keywords);
    }

    public List<String> getAliases() {
        return List.copyOf(aliases);
    }

    public boolean isActive() {
        return active;
    }
}