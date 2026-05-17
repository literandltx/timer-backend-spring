package com.literandltx.reportservice.listener;

import com.literandltx.reportservice.event.ReportRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportRequestedEventListener {

    @KafkaListener(topics = "${app.kafka.topics.report}")
    public void onReportRequested(ReportRequestedEvent event) {
        log.info("Report event received: {}", event);
    }

}
