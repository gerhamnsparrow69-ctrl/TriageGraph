package in.ac.bitspilani.triagegraph.triage.infrastructure;

import java.util.List;

import org.springframework.stereotype.Repository;

import in.ac.bitspilani.triagegraph.triage.domain.KeywordRepository;
import in.ac.bitspilani.triagegraph.triage.domain.TriageKeyword;

@Repository
class JpaKeywordRepository implements KeywordRepository {

    private final SpringDataTriageKeywordRepository jpa;

    JpaKeywordRepository(SpringDataTriageKeywordRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public List<TriageKeyword> findAll() {
        return jpa.findAll();
    }

    @Override
    public TriageKeyword save(TriageKeyword keyword) {
        return jpa.save(keyword);
    }

    @Override
    public void deleteById(long id) {
        jpa.deleteById(id);
    }

    @Override
    public boolean existsById(long id) {
        return jpa.existsById(id);
    }

    @Override
    public long count() {
        return jpa.count();
    }
}
