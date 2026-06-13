package com.literandltx.timer.service.impl;

import com.literandltx.timer.dto.sync.SyncAction;
import com.literandltx.timer.dto.sync.SyncMessage;
import com.literandltx.timer.service.WebSocketBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketBroadcastServiceImpl implements WebSocketBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public <T> void broadcast(String userEmail, String destination, SyncAction action, T payload) {
        log.debug("Broadcasting {} to {} for user: {}", action, destination, userEmail);
        
        SyncMessage<T> message = new SyncMessage<>(action, payload);
        
        messagingTemplate.convertAndSendToUser(
                userEmail,
                destination,
                message
        );
    }

}
