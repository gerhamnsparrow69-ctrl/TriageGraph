package in.ac.bitspilani.triagegraph.triage.domain;

import java.util.List;

/** Source of the current keyword rules (the database, with defaults on first use). */
@FunctionalInterface
public interface KeywordRules {

    List<KeywordRule> all();
}
