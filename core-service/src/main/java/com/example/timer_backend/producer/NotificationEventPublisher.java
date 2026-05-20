package com.example.timer_backend.producer;

import com.example.timer_backend.event.NotificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.notification}")
    private String notificationTopic;

    public void publishNotificationRequest(NotificationRequestedEvent event) {
        String kafkaKey = String.valueOf(event.getUserId());
        
        kafkaTemplate.send(notificationTopic, kafkaKey, event);
        log.info("Published notification request for user ID: {}", event.getUserId());
    }

}
