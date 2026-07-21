package com.taskflow.taskservice.kafka;

import com.taskflow.taskservice.dto.NotificationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Publishes notification events to the Kafka topic {@value #TOPIC}.
 * task-service uses this instead of a synchronous Feign call so that
 * task operations remain fast even when notification-service is unavailable.
 */
@Component
public class NotificationKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaProducer.class);
    public static final String TOPIC = "task-notifications";

    private final KafkaTemplate<String, NotificationDto> kafkaTemplate;

    public NotificationKafkaProducer(KafkaTemplate<String, NotificationDto> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a notification event asynchronously.
     * Any send failure is logged but never propagated to the caller.
     */
    public void send(NotificationDto notification) {
        CompletableFuture<SendResult<String, NotificationDto>> future =
                kafkaTemplate.send(TOPIC, notification.getUserId(), notification);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send notification to Kafka [userId={}, msg={}]: {}",
                        notification.getUserId(), notification.getMessage(), ex.getMessage());
            } else {
                log.debug("Notification sent to Kafka [topic={}, partition={}, offset={}]",
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
