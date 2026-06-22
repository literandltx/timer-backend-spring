package com.literandltx.timer.dto.settings;

import java.time.LocalDateTime;
import java.util.UUID;

public record TimerSettingRequestDto(
        UUID uuid,
        UUID timerOptionUuid,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
