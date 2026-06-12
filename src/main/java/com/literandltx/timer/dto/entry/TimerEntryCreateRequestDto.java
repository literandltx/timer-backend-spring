package com.literandltx.timer.dto.entry;

import java.time.LocalDateTime;
import java.util.UUID;

public record TimerEntryCreateRequestDto(
        UUID uuid,
        UUID labelId,
        Long durationSeconds,
        Long startTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}


