package com.example.timer_backend.dto.timer.entry;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTimerEntryRequestDto {

    @NotNull(message = "Label ID is required")
    private Long labelId;

    @NotNull(message = "Duration is required")
    @PositiveOrZero(message = "Duration cannot be negative")
    private Long durationSeconds;

    @NotNull(message = "Start time is required")
    @PositiveOrZero(message = "Start time must be a valid timestamp")
    private Long startTime;
}
