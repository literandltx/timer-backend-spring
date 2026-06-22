package com.literandltx.timer.service;

import com.literandltx.timer.dto.sync.SyncAction;

public interface WebSocketBroadcastService {

    /**
     * Broadcasts a message to a specific user's active WebSocket sessions.
     *
     * @param userEmail   The identifier of the user (must match the JWT Principal)
     * @param destination The WebSocket topic/queue (e.g., "/queue/labels")
     * @param action      CREATE, UPDATE, or DELETE, see {@link SyncAction}
     * @param payload     The actual ResponseDto
     */
    <T> void broadcast(String userEmail, String destination, SyncAction action, T payload);

}
