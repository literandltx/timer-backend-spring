package com.literandltx.timer.dto.label;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record LabelCreateRequestDto(
        UUID uuid,
        String name,
        String color,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
