package com.literandltx.timer.controller;

import com.literandltx.timer.dto.entry.TimerEntryCreateRequestDto;
import com.literandltx.timer.dto.entry.TimerEntryResponseDto;
import com.literandltx.timer.dto.entry.TimerEntryUpdateRequestDto;
import com.literandltx.timer.model.User;
import com.literandltx.timer.service.TimerEntryService;
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
@RequestMapping("/api/v1/timer-entries")
public class TimerEntryController {

    private final TimerEntryService timerEntryService;

    @PostMapping
    public ResponseEntity<TimerEntryResponseDto> save(
            @RequestBody TimerEntryCreateRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        TimerEntryResponseDto response = timerEntryService.save(request, user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<TimerEntryResponseDto>> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAfter,
            @AuthenticationPrincipal User user
    ) {
        List<TimerEntryResponseDto> labels = timerEntryService.findAll(updatedAfter, user);
        return ResponseEntity.ok(labels);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TimerEntryResponseDto> updateLabel(
            @PathVariable UUID id,
            @RequestBody TimerEntryUpdateRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        TimerEntryResponseDto updatedLabel = timerEntryService.update(id, request, user);
        return ResponseEntity.ok(updatedLabel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        timerEntryService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

}
