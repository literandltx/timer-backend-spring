package com.example.timer_backend.dto.export;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"labelName", "color", "durationSeconds", "startTime"})
public class TimerEntryCsvDto {
    @JsonProperty("labelName")
    private String labelName;

    @JsonProperty("color")
    private String color;

    @JsonProperty("durationSeconds")
    private Long durationSeconds;
    
    @JsonProperty("startTime")
    private Long startTime;
}
