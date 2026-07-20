package com.literandltx.timer.dto.preset;

import java.time.LocalDateTime;
import java.util.UUID;

public record TimerPresetResponseDto(
        UUID uuid,
        UUID labelUuid,
        UUID timerOptionUuid,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {
}
