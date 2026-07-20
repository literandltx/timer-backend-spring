package com.literandltx.timer.controller;

import com.literandltx.timer.dto.preset.TimerPresetRequestDto;
import com.literandltx.timer.dto.preset.TimerPresetResponseDto;
import com.literandltx.timer.model.User;
import com.literandltx.timer.service.TimerPresetService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/timer-presets")
@RequiredArgsConstructor
public class TimerPresetController {

    private final TimerPresetService timerPresetService;

    @PutMapping
    public ResponseEntity<TimerPresetResponseDto> pushSettings(
            @RequestBody TimerPresetRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        TimerPresetResponseDto response = timerPresetService.upsert(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sync")
    public ResponseEntity<TimerPresetResponseDto> pullSettings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAfter,
            @AuthenticationPrincipal User user
    ) {
        TimerPresetResponseDto response = timerPresetService.find(updatedAfter, user);

        if (response == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        return ResponseEntity.ok(response);
    }

}
