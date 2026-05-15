package com.example.timer_backend.controller;

import com.example.timer_backend.dto.label.CreateLabelRequestDto;
import com.example.timer_backend.dto.label.CreateLabelResponseDto;
import com.example.timer_backend.dto.label.LabelRequestDto;
import com.example.timer_backend.dto.label.LabelResponseDto;
import com.example.timer_backend.model.User;
import com.example.timer_backend.service.LabelService;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/labels")
public class LabelController {
    private final LabelService labelService;

    @PostMapping
    public ResponseEntity<CreateLabelResponseDto> save(
            @RequestBody CreateLabelRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        CreateLabelResponseDto response = labelService.save(request, user);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    @ResponseBody
    public ResponseEntity<List<LabelResponseDto>> findAll(
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        List<LabelResponseDto> response = labelService.findAll(user, pageable);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LabelResponseDto> findById(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        LabelResponseDto response = labelService.findById(id, user);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LabelResponseDto> update(
            @PathVariable Long id,
            @RequestBody LabelRequestDto request,
            @AuthenticationPrincipal User user
    ) {
        LabelResponseDto response = labelService.updateById(id, request, user);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        labelService.deleteById(id, user);
        return ResponseEntity
                .noContent()
                .build();
    }
}
