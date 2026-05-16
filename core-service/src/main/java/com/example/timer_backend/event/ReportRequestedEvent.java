package com.example.timer_backend.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportRequestedEvent {
    private Long userId;
    private String email;
    private String reportType;
}
