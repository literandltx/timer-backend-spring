package com.literandltx.reportservice.service;

import com.literandltx.reportservice.event.ReportRequestedEvent;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Service
public class ReportGeneratorService {

    public byte[] generateTxtReport(ReportRequestedEvent event) {
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

}
