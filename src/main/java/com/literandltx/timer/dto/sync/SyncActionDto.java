package com.literandltx.timer.dto.sync;

import java.util.Map;
import java.util.UUID;
import lombok.Data;

@Data
public class SyncActionDto {
    private Long id;
    private UUID entityId;
    private String entityType;
    private String action;
    private Map<String, Object> payload;
    private Long timestamp;
}
