package com.literandltx.timer.dto.option;

import java.time.LocalDateTime;

public record TimerOptionUpdateRequestDto(
        Long value,
        LocalDateTime updatedAt
) {
}
