package com.merchantonboarding.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import com.merchantonboarding.event.ComplianceResponseEvent;

@Configuration
public class KafkaConfig {

    @Value("${app.kafka.topics.verification-request}")
    private String verificationRequestTopic;

    @Value("${app.kafka.topics.verification-response}")
    private String verificationResponseTopic;

    @Value("${app.kafka.topics.compliance-request}")
    private String complianceRequestTopic;

    @Value("${app.kafka.topics.compliance-response}")
    private String complianceResponseTopic;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

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

    @Bean
    public NewTopic complianceRequestTopic() {
        return TopicBuilder.name(complianceRequestTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic complianceResponseTopic() {
        return TopicBuilder.name(complianceResponseTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // Separate consumer factory for compliance responses (different event type)
    @Bean
    public ConsumerFactory<String, ComplianceResponseEvent> complianceConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "merchant-onboarding-compliance-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.merchantonboarding.event");
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ComplianceResponseEvent.class.getName());
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ComplianceResponseEvent> complianceKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ComplianceResponseEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(complianceConsumerFactory());
        return factory;
    }
}
