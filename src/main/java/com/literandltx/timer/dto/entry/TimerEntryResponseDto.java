package com.literandltx.timer.dto.entry;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TimerEntryResponseDto(
        UUID uuid,
        UUID labelId,
        Long durationSeconds,
        Long startTime,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {
}
