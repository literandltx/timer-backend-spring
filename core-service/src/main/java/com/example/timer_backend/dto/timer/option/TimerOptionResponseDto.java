package com.example.timer_backend.dto.timer.option;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimerOptionResponseDto {
    private Long id;
    private Long userId;
    private Long value;
}
