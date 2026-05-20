package com.literandltx.reportservice.service.strategy;

import com.literandltx.reportservice.event.ReportRequestedEvent;

public interface ReportGeneratorStrategy {
    byte[] generate(ReportRequestedEvent event);

    String getSupportedFormat();
}
