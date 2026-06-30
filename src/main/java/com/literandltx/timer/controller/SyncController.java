package com.literandltx.timer.controller;

import com.literandltx.timer.dto.sync.SyncQueueBulkRequest;
import com.literandltx.timer.model.User;
import com.literandltx.timer.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sync")
public class SyncController {

    private SyncService syncService;

    @PostMapping("/queue")
    public ResponseEntity<Void> processQueue(
            @AuthenticationPrincipal User user,
            @RequestBody SyncQueueBulkRequest request
    ) {
        syncService.processQueue(request, user);

        return ResponseEntity.accepted().build();
    }

}
