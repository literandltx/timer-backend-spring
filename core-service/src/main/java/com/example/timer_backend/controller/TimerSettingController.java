package com.example.timer_backend.controller;

import com.example.timer_backend.dto.timer.setting.CreateTimerSettingRequestDto;
import com.example.timer_backend.dto.timer.setting.CreateTimerSettingResponseDto;
import com.example.timer_backend.dto.timer.setting.TimerSettingRequestDto;
import com.example.timer_backend.dto.timer.setting.TimerSettingResponseDto;
import com.example.timer_backend.model.User;
import com.example.timer_backend.service.TimerSettingService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/timer-settings")
public class TimerSettingController {
    private final TimerSettingService timerSettingService;

    @PostMapping
    public ResponseEntity<CreateTimerSettingResponseDto> save(
            @RequestBody @Valid CreateTimerSettingRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        CreateTimerSettingResponseDto response = timerSettingService.save(request, user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TimerSettingResponseDto>> findAll(
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        List<TimerSettingResponseDto> response = timerSettingService.findAll(user, pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimerSettingResponseDto> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        TimerSettingResponseDto response = timerSettingService.findById(id, user);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimerSettingResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid TimerSettingRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        TimerSettingResponseDto response = timerSettingService.updateById(id, request, user);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        timerSettingService.deleteById(id, user);
        return ResponseEntity
                .noContent()
                .build();
    }
}
