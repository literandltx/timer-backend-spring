package com.example.timer_backend.dto.timer.option;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTimerOptionRequestDto {
    @NotNull(message = "Timer value cannot be null")
    @Min(value = 1, message = "Timer value must be at least 1 minute")
    private Long value;
}
