package com.literandltx.timer.controller;

import com.literandltx.timer.dto.label.LabelCreateRequestDto;
import com.literandltx.timer.dto.label.LabelResponseDto;
import com.literandltx.timer.dto.label.LabelUpdateRequestDto;
import com.literandltx.timer.model.User;
import com.literandltx.timer.service.LabelService;
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
@RequestMapping("/api/v1/labels")
public class LabelController {

    private final LabelService labelService;

    @PostMapping
    public ResponseEntity<LabelResponseDto> save(
            @RequestBody LabelCreateRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        LabelResponseDto response = labelService.save(request, user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<LabelResponseDto>> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAfter,
            @AuthenticationPrincipal User user
    ) {
        List<LabelResponseDto> labels = labelService.findAll(updatedAfter, user);
        return ResponseEntity.ok(labels);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabelResponseDto> updateLabel(
            @PathVariable UUID id,
            @RequestBody LabelUpdateRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        LabelResponseDto updatedLabel = labelService.update(id, request, user);
        return ResponseEntity.ok(updatedLabel);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLabel(
            @PathVariable UUID id,
            @AuthenticationPrincipal User user
    ) {
        labelService.delete(id, user);
        return ResponseEntity.noContent().build();
    }

}
