package com.example.timer_backend.controller;

import com.example.timer_backend.dto.timer.option.CreateTimerOptionRequestDto;
import com.example.timer_backend.dto.timer.option.CreateTimerOptionResponseDto;
import com.example.timer_backend.dto.timer.option.TimerOptionRequestDto;
import com.example.timer_backend.dto.timer.option.TimerOptionResponseDto;
import com.example.timer_backend.model.User;
import com.example.timer_backend.service.TimerOptionService;
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
@RequestMapping("/api/v1/timer-options")
public class TimerOptionController {
    private final TimerOptionService timerOptionService;

    @PostMapping
    public ResponseEntity<CreateTimerOptionResponseDto> save(
            @RequestBody @Valid CreateTimerOptionRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        CreateTimerOptionResponseDto response = timerOptionService.save(request, user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TimerOptionResponseDto>> findAll(
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        List<TimerOptionResponseDto> response = timerOptionService.findAll(user, pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TimerOptionResponseDto> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        TimerOptionResponseDto response = timerOptionService.findById(id, user);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimerOptionResponseDto> update(
            @PathVariable Long id,
            @RequestBody @Valid TimerOptionRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        TimerOptionResponseDto response = timerOptionService.updateById(id, request, user);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        timerOptionService.deleteById(id, user);
        return ResponseEntity
                .noContent()
                .build();
    }
}
