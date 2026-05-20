package com.example.timer_backend.event;

import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportRequestedEvent {
    private UUID reportId;
    private Long userId;
    private String email;
    private String reportType;
    private Map<String, Object> filters;
}
