package com.literandltx.reportservice.service.strategy;

import com.literandltx.reportservice.event.ReportRequestedEvent;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;

@Component
public class PdfReportGeneratorStrategy implements ReportGeneratorStrategy {

    @Override
    public byte[] generate(ReportRequestedEvent event) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font courier = FontFactory.getFont(FontFactory.COURIER, 12);

            document.add(new Paragraph("==================================================", courier));
            document.add(new Paragraph("                   ACTIVITY REPORT                ", courier));
            document.add(new Paragraph("==================================================", courier));
            document.add(new Paragraph("Generated At : " + LocalDateTime.now(), courier));
            document.add(new Paragraph("Report ID    : " + event.getReportId(), courier));
            document.add(new Paragraph("Report Type  : " + event.getReportType(), courier));
            document.add(new Paragraph("User ID      : " + event.getUserId(), courier));
            document.add(new Paragraph("User Email   : " + event.getEmail(), courier));
            document.add(new Paragraph("==================================================", courier));

            document.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report", e);
        }
    }

    @Override
    public String getSupportedFormat() {
        return "PDF";
    }
}
