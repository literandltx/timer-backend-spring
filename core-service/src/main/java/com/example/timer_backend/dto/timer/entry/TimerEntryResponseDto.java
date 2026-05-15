package com.example.timer_backend.dto.timer.entry;

import lombok.Data;

@Data
public class TimerEntryResponseDto {
    private Long id;
    private Long userId;
    private Long labelId;
    private Long durationSeconds;
    private Long startTime;
}
