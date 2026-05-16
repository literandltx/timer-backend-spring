package com.example.timer_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.notification}")
    private String notificationTopic;

    @Value("${app.kafka.topics.report}")
    private String reportTopic;

    @Bean
    public KafkaAdmin.NewTopics createMultipleTopics() {
        return new KafkaAdmin.NewTopics(
                TopicBuilder.name(notificationTopic)
                        .partitions(1)
                        .replicas(1)
                        .build(),

                TopicBuilder.name(reportTopic)
                        .partitions(3)
                        .replicas(1)
                        .build()
        );
    }

}
