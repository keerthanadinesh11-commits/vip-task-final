package com.taskflow.taskservice.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Ensures the {@value NotificationKafkaProducer#TOPIC} topic exists when
 * task-service starts up. Kafka will create it automatically (if
 * auto.create.topics.enable=true), but declaring it here lets us pin
 * the partition count and replication factor explicitly.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic taskNotificationsTopic() {
        return TopicBuilder.name(NotificationKafkaProducer.TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
