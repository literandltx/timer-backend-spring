package com.example.timer_backend.dto.report;

import java.util.Map;
import lombok.Data;

@Data
public class ReportRequestDto {
    private String reportType;
    private Map<String, Object> filters;
}
