package com.literandltx.timer.dto.option;

import java.time.LocalDateTime;
import java.util.UUID;

public record TimerOptionResponseDto(
        UUID uuid,
        Long value,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {
}
