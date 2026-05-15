package com.example.timer_backend.dto.label;

import lombok.Data;

@Data
public class LabelResponseDto {
    private Long id;
    private Long userId;
    private String name;
    private String color;
}
