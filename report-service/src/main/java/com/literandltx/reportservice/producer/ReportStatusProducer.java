package com.literandltx.reportservice.producer;

import com.literandltx.reportservice.event.ReportStatusEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportStatusProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.core}")
    private String coreTopic;

    public void sendStatusUpdate(Long userId, ReportStatusEvent event) {
        String key = String.valueOf(userId);
        log.info("Publishing report status [{}] for report ID: {} to topic: {}",
                event.getStatus(), event.getReportId(), coreTopic);

        kafkaTemplate.send(coreTopic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to deliver report status event for report ID: {}", event.getReportId(), ex);
                    } else {
                        log.debug("Delivered report status event for report ID: {}", event.getReportId());
                    }
                });
    }
}
