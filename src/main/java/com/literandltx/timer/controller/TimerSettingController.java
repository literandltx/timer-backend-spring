package com.literandltx.timer.controller;

import com.literandltx.timer.dto.settings.TimerSettingRequestDto;
import com.literandltx.timer.dto.settings.TimerSettingResponseDto;
import com.literandltx.timer.model.User;
import com.literandltx.timer.service.TimerSettingService;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class TimerSettingController {

    private final TimerSettingService timerSettingService;

    @PutMapping
    public ResponseEntity<TimerSettingResponseDto> pushSettings(
            @RequestBody TimerSettingRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        TimerSettingResponseDto response = timerSettingService.upsert(request, user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sync")
    public ResponseEntity<TimerSettingResponseDto> pullSettings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAfter,
            @AuthenticationPrincipal User user
    ) {
        TimerSettingResponseDto response = timerSettingService.find(updatedAfter, user);
        return ResponseEntity.ok(response);
    }
}
