package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.List;

public interface KeywordRepository {

    List<TriageKeyword> findAll();

    TriageKeyword save(TriageKeyword keyword);

    void deleteById(long id);

    boolean existsById(long id);

    long count();
}
