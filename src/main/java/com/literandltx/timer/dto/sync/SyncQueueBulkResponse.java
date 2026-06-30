package com.literandltx.timer.dto.sync;

import java.util.List;
import lombok.Data;

@Data
public class SyncQueueBulkResponse {
    private List<Long> successfulIds;
    private List<FailedSyncAction> failedActions;

    @Data
    public static class FailedSyncAction {
        private Long id;
        private String error;
        
        public FailedSyncAction(Long id, String error) {
            this.id = id;
            this.error = error;
        }
    }
}
