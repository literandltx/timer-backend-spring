package com.literandltx.timer.dto.entry;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TimerEntryUpdateRequestDto(
        UUID labelId,
        Long durationSeconds,
        Long startTime,
        LocalDateTime updatedAt
) {
}
