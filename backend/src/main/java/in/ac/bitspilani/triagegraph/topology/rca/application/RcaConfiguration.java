package in.ac.bitspilani.triagegraph.topology.rca.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import in.ac.bitspilani.triagegraph.topology.rca.domain.CandidateGenerator;
import in.ac.bitspilani.triagegraph.topology.rca.domain.EvidenceAggregator;
import in.ac.bitspilani.triagegraph.topology.rca.domain.RcaConfig;
import in.ac.bitspilani.triagegraph.topology.rca.domain.RcaScorer;

@Configuration
public class RcaConfiguration {

    @Bean
    RcaConfig rcaConfig() {
        return RcaConfig.defaults();
    }

    @Bean
    CandidateGenerator candidateGenerator() {
        return new CandidateGenerator();
    }

    @Bean
    EvidenceAggregator evidenceAggregator(RcaConfig config) {
        return new EvidenceAggregator(config.maximumEvidenceBonus());
    }

    @Bean
    RcaScorer rcaScorer(RcaConfig config) {
        return new RcaScorer(config);
    }
}
