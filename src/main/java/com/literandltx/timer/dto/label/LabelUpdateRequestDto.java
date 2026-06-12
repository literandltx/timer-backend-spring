package com.literandltx.timer.dto.label;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record LabelUpdateRequestDto(
        String name,
        String color,
        LocalDateTime updatedAt
) {
}
