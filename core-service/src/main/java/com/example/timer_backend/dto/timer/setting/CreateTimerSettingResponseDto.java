package com.example.timer_backend.dto.timer.setting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTimerSettingResponseDto {
    private Long id;
    private Long userId;
    private Long timerOptionId;
    private Long value;
    private Long lastUpdated;
}
