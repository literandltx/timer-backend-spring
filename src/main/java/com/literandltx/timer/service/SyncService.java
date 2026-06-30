package com.literandltx.timer.service;

import com.literandltx.timer.dto.sync.SyncQueueBulkRequest;
import com.literandltx.timer.dto.sync.SyncQueueBulkResponse;
import com.literandltx.timer.model.User;
import org.springframework.stereotype.Service;

@Service
public interface SyncService {

    SyncQueueBulkResponse processQueue(SyncQueueBulkRequest request, User user);

}
