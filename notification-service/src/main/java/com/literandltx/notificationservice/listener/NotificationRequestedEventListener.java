package com.literandltx.notificationservice.listener;

import com.literandltx.notificationservice.event.NotificationRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationRequestedEventListener {

    @KafkaListener(topics = "${app.kafka.topics.notification}")
    public void onReportRequested(NotificationRequestedEvent event) {
        log.info("Notification event received: {}", event);
    }

}
