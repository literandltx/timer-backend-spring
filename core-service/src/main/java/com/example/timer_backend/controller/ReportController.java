package com.example.timer_backend.controller;

import com.example.timer_backend.event.ReportRequestedEvent;
import com.example.timer_backend.model.User;
import com.example.timer_backend.producer.ReportEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/reports")
public class ReportController {
    private final ReportEventPublisher eventPublisher;

    @PostMapping
    public ResponseEntity<Void> requestReport(@AuthenticationPrincipal User user) {
        ReportRequestedEvent event = new ReportRequestedEvent(
                user.getId(),
                user.getEmail(),
                "USER_ACTIVITY_REPORT"
        );

        eventPublisher.publishReportRequest(event);

        return ResponseEntity.accepted().build();
    }

}
