package com.example.timer_backend.dto.timer.entry;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class TimerEntryRequestDto {

    @NotNull(message = "Label ID is required")
    private Long labelId;

    @PositiveOrZero(message = "Duration cannot be negative")
    private Long durationSeconds;

    @NotNull(message = "Start time is required")
    private Long startTime;
}
