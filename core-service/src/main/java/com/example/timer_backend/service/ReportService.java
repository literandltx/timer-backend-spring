package com.example.timer_backend.service;

import com.example.timer_backend.dto.report.ReportRequestDto;
import com.example.timer_backend.dto.report.ReportStatusResponseDto;
import com.example.timer_backend.model.User;
import org.springframework.stereotype.Service;

@Service
public interface ReportService {
    ReportStatusResponseDto requestReport(User user, ReportRequestDto reportRequestDto);
}
