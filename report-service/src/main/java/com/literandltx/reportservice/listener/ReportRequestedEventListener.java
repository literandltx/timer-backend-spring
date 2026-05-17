package com.literandltx.reportservice.listener;

import com.literandltx.reportservice.event.ReportRequestedEvent;
import com.literandltx.reportservice.event.ReportStatusEvent;
import com.literandltx.reportservice.producer.ReportStatusProducer;
import com.literandltx.reportservice.service.ReportGeneratorService;
import com.literandltx.reportservice.service.S3UploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportRequestedEventListener {
    private final S3UploadService s3UploadService;
    private final ReportGeneratorService reportGeneratorService;
    private final ReportStatusProducer reportStatusProducer;

    @KafkaListener(topics = "${app.kafka.topics.report}")
    public void onReportRequested(ReportRequestedEvent event) {
        log.info("Received execution job for report ID: {}", event.getReportId());

        String s3Key = String.format("reports/%d/%s.txt", event.getUserId(), event.getReportId());

        try {
            byte[] txtReportBytes = reportGeneratorService.generateTxtReport(event);
            s3UploadService.uploadFile(s3Key, txtReportBytes);

            ReportStatusEvent successEvent = new ReportStatusEvent(
                    event.getReportId(),
                    "COMPLETED",
                    s3Key,
                    ""
            );

            reportStatusProducer.sendStatusUpdate(event.getUserId(), successEvent);
            log.info("Compiled TXT file with ID: {}", event.getReportId());
        } catch (Exception ex) {
            log.error("Failed handling report compilation job for ID: {}", event.getReportId(), ex);

            ReportStatusEvent failureEvent = new ReportStatusEvent(
                    event.getReportId(),
                    "FAILED",
                    null,
                    ex.getMessage()
            );

            reportStatusProducer.sendStatusUpdate(event.getUserId(), failureEvent);
        }
    }
}
