package com.taskflow.notificationservice.kafka;

import com.taskflow.notificationservice.dto.NotificationEventDto;
import com.taskflow.notificationservice.entity.Notification;
import com.taskflow.notificationservice.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens on the {@code task-notifications} Kafka topic and persists each
 * incoming notification event as a {@link Notification} entity.
 */
@Component
public class NotificationKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationKafkaConsumer.class);
    public static final String TOPIC = "task-notifications";

    private final NotificationService notificationService;

    public NotificationKafkaConsumer(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = TOPIC,
            groupId = "${spring.kafka.consumer.group-id:notification-service-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, NotificationEventDto> record) {
        NotificationEventDto event = record.value();
        if (event == null) {
            log.warn("Received null Kafka record value at offset {}", record.offset());
            return;
        }

        log.info("Received notification event [userId={}, topic={}, partition={}, offset={}]",
                event.getUserId(), record.topic(), record.partition(), record.offset());

        try {
            Notification notification = new Notification();
            notification.setMessage(event.getMessage());
            notification.setUserId(event.getUserId());
            notificationService.save(notification);
            log.debug("Notification persisted for userId={}", event.getUserId());
        } catch (Exception ex) {
            log.error("Failed to persist notification for userId={}: {}",
                    event.getUserId(), ex.getMessage(), ex);
        }
    }
}
