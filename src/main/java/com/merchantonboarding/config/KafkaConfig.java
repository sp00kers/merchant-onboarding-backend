package com.merchantonboarding.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.verification-request}")
    private String verificationRequestTopic;

    @Value("${app.kafka.topics.verification-response}")
    private String verificationResponseTopic;

    @Bean
    public NewTopic verificationRequestTopic() {
        return TopicBuilder.name(verificationRequestTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic verificationResponseTopic() {
        return TopicBuilder.name(verificationResponseTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
