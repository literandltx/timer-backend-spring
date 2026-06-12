package com.literandltx.timer.dto.option;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record TimerOptionResponseDto(
        UUID uuid,
        Long value,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {
}
