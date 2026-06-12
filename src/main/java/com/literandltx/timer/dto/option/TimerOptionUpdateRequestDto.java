package com.literandltx.timer.dto.option;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record TimerOptionUpdateRequestDto(
        Long value,
        LocalDateTime updatedAt
) {
}
