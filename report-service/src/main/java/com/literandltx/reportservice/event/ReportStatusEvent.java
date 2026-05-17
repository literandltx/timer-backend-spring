package com.literandltx.reportservice.event;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReportStatusEvent {
    private UUID reportId;
    private String status;
    private String s3Key;
    private String errorMessage;
}
