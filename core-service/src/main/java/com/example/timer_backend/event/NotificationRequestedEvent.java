package com.example.timer_backend.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NotificationRequestedEvent {
    private Long userId;
    private String email;
    private String notificationType;
}
