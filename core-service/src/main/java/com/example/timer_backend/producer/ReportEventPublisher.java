package com.example.timer_backend.producer;

import com.example.timer_backend.event.ReportRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportEventPublisher {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topics.report}")
    private String reportTopic;

    public void publishReportRequest(ReportRequestedEvent event) {
        String kafkaKey = String.valueOf(event.getUserId());
        
        kafkaTemplate.send(reportTopic, kafkaKey, event);
        log.info("Published report request for user ID: {}", event.getUserId());
    }

}
