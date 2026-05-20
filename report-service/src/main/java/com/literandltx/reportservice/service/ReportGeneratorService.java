package com.literandltx.reportservice.service;

import com.literandltx.reportservice.event.ReportRequestedEvent;
import com.literandltx.reportservice.service.strategy.ReportGeneratorStrategy;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportGeneratorService {

    private final Map<String, ReportGeneratorStrategy> strategies;

    public ReportGeneratorService(List<ReportGeneratorStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        strategy -> strategy.getSupportedFormat().toUpperCase(),
                        Function.identity()
                ));
    }

    public byte[] generateReport(ReportRequestedEvent event) {
        ReportGeneratorStrategy strategy = strategies.get(event.getReportType().toUpperCase());

        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported report format: " + event.getReportType());
        }

        return strategy.generate(event);
    }
}
