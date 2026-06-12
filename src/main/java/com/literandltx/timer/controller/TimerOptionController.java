package com.literandltx.timer.controller;

import com.literandltx.timer.dto.option.TimerOptionCreateRequestDto;
import com.literandltx.timer.dto.option.TimerOptionResponseDto;
import com.literandltx.timer.dto.option.TimerOptionUpdateRequestDto;
import com.literandltx.timer.model.User;
import com.literandltx.timer.service.TimerOptionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/timer-options")
public class TimerOptionController {

    private final TimerOptionService timerOptionService;

    @PostMapping
    public ResponseEntity<TimerOptionResponseDto> save(
            @RequestBody TimerOptionCreateRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        TimerOptionResponseDto response = timerOptionService.save(request, user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TimerOptionResponseDto>> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAfter,
            @AuthenticationPrincipal User user
    ) {
        List<TimerOptionResponseDto> labels = timerOptionService.findAll(updatedAfter, user);
        return ResponseEntity.ok(labels);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimerOptionResponseDto> updateLabel(
            @PathVariable UUID id,
            @RequestBody TimerOptionUpdateRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        TimerOptionResponseDto updatedLabel = timerOptionService.update(id, request, user);
        return ResponseEntity.ok(updatedLabel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        timerOptionService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

}
