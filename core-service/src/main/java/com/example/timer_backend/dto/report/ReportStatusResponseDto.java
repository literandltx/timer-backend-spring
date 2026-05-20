package com.example.timer_backend.dto.report;

import java.util.UUID;
import lombok.Data;

@Data
public class ReportStatusResponseDto {
    private UUID id;
    private String status;
}
