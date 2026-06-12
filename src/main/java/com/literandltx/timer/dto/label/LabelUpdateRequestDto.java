package com.literandltx.timer.dto.label;

import java.time.LocalDateTime;

public record LabelUpdateRequestDto(
        String name,
        String color,
        LocalDateTime updatedAt
) {
}
