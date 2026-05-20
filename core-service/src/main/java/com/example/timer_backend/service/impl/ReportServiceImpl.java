package com.example.timer_backend.service.impl;

import com.example.timer_backend.dto.report.ReportRequestDto;
import com.example.timer_backend.dto.report.ReportStatusResponseDto;
import com.example.timer_backend.event.ReportRequestedEvent;
import com.example.timer_backend.model.Report;
import com.example.timer_backend.model.ReportStatus;
import com.example.timer_backend.model.User;
import com.example.timer_backend.producer.ReportEventPublisher;
import com.example.timer_backend.repository.ReportRepository;
import com.example.timer_backend.service.ReportService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final ReportEventPublisher eventPublisher;

    @Override
    @Transactional
    public ReportStatusResponseDto requestReport(
            User user,
            ReportRequestDto reportRequest
    ) {
        Report report = new Report();
        report.setUserId(user.getId());
        report.setReportType(reportRequest.getReportType());
        report.setStatus(ReportStatus.PENDING);

        report = reportRepository.save(report);

        ReportRequestedEvent event = new ReportRequestedEvent();
        event.setReportId(report.getId());
        event.setUserId(user.getId());
        event.setEmail(user.getEmail());
        event.setReportType(reportRequest.getReportType());
        event.setFilters(reportRequest.getFilters());

        eventPublisher.publishReportRequest(event);

        ReportStatusResponseDto response = new ReportStatusResponseDto();
        response.setId(report.getId());
        response.setStatus(report.getStatus().toString());

        return response;
    }
}
