package com.literandltx.timer.dto.option;

import java.time.LocalDateTime;
import java.util.UUID;

public record TimerOptionCreateRequestDto(
        UUID uuid,
        Long value,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}


