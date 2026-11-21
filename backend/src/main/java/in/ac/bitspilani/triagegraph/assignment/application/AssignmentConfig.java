package in.ac.bitspilani.triagegraph.assignment.application;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentStrategy;
import in.ac.bitspilani.triagegraph.assignment.domain.AssignmentStrategyRegistry;
import in.ac.bitspilani.triagegraph.assignment.domain.LoadBalancedStrategy;
import in.ac.bitspilani.triagegraph.assignment.domain.RoundRobinStrategy;
import in.ac.bitspilani.triagegraph.assignment.domain.ServiceNameLookup;
import in.ac.bitspilani.triagegraph.assignment.domain.SkillBasedStrategy;
import in.ac.bitspilani.triagegraph.assignment.domain.SmartScoreStrategy;

/**
 * Wires the strategies. The domain classes stay free of Spring; adding a strategy means adding one
 * {@code @Bean} here (or a {@code @Component} class), and the registry picks it up automatically.
 */
@Configuration
public class AssignmentConfig {

    @Bean
    SmartScoreStrategy smartScoreStrategy(ServiceNameLookup serviceNames, AssignmentProperties props) {
        return new SmartScoreStrategy(serviceNames, props.experienceCap());
    }

    @Bean
    SkillBasedStrategy skillBasedStrategy() {
        return new SkillBasedStrategy();
    }

    @Bean
    LoadBalancedStrategy loadBalancedStrategy() {
        return new LoadBalancedStrategy();
    }

    @Bean
    RoundRobinStrategy roundRobinStrategy() {
        return new RoundRobinStrategy();
    }

    @Bean
    AssignmentStrategyRegistry assignmentStrategyRegistry(List<AssignmentStrategy> strategies) {
        return new AssignmentStrategyRegistry(strategies);
    }
}
