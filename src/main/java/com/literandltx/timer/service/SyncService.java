package com.literandltx.timer.service;

import com.literandltx.timer.dto.sync.SyncQueueBulkRequest;
import com.literandltx.timer.model.User;
import org.springframework.stereotype.Service;

@Service
public interface SyncService {

    boolean processQueue(SyncQueueBulkRequest request, User user);

}
