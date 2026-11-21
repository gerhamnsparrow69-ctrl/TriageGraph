package in.ac.bitspilani.triagegraph.notification.application;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import in.ac.bitspilani.triagegraph.notification.domain.NotificationChannel;

@Configuration
class NotificationConfig {

    @Bean
    Sleeper notificationSleeper() {
        return Sleeper.REAL;
    }

    @Bean
    NotificationChannelFactory notificationChannelFactory(List<NotificationChannel> channels, NotifyProperties props,
            Sleeper sleeper) {
        return new NotificationChannelFactory(channels, props, sleeper);
    }

    @Bean
    ChannelSelectionPolicy channelSelectionPolicy() {
        return new RuleTableChannelSelectionPolicy(RuleTableChannelSelectionPolicy.DEFAULT_RULES);
    }
}
