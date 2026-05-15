package com.example.timer_backend.dto.timer.setting;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerSettingRequestDto {
    @NotNull(message = "Timer Option ID cannot be null")
    private Long timerOptionId;
}
