package com.literandltx.reportservice.service.strategy;

import com.literandltx.reportservice.event.ReportRequestedEvent;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class TxtReportGeneratorStrategy implements ReportGeneratorStrategy {

    @Override
    public byte[] generate(ReportRequestedEvent event) {
        StringBuilder builder = new StringBuilder();

        builder.append("==================================================\n");
        builder.append("                   ACTIVITY REPORT                \n");
        builder.append("==================================================\n");
        builder.append("Generated At : ").append(LocalDateTime.now()).append("\n");
        builder.append("Report ID    : ").append(event.getReportId()).append("\n");
        builder.append("Report Type  : ").append(event.getReportType()).append("\n");
        builder.append("User ID      : ").append(event.getUserId()).append("\n");
        builder.append("User Email   : ").append(event.getEmail()).append("\n");
        builder.append("==================================================\n");

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getSupportedFormat() {
        return "TXT";
    }
}
