package com.example.timer_backend.controller;

import com.example.timer_backend.dto.report.ReportRequestDto;
import com.example.timer_backend.dto.report.ReportStatusResponseDto;
import com.example.timer_backend.model.User;
import com.example.timer_backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ReportStatusResponseDto> requestReport(
            @AuthenticationPrincipal User user,
            @RequestBody ReportRequestDto reportRequestDto
    ) {
        ReportStatusResponseDto report = reportService.requestReport(user, reportRequestDto);
        return ResponseEntity.accepted().body(report);
    }

}
