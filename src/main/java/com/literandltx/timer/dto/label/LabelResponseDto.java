package com.literandltx.timer.dto.label;

import java.time.LocalDateTime;
import java.util.UUID;

public record LabelResponseDto(
        UUID uuid,
        String name,
        String color,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean deleted
) {
}
