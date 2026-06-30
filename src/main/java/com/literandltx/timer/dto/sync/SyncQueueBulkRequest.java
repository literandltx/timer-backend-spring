package com.literandltx.timer.dto.sync;

import java.util.List;
import lombok.Data;

@Data
public class SyncQueueBulkRequest {
    private List<SyncActionDto> actions;
}
