package com.example.timer_backend.controller;

import com.example.timer_backend.dto.user.ChangeEmailRequestDto;
import com.example.timer_backend.dto.user.ChangePasswordRequestDto;
import com.example.timer_backend.dto.user.UserResponseDto;
import com.example.timer_backend.dto.user.UserUpdateRequestDto;
import com.example.timer_backend.model.User;
import com.example.timer_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(
            @AuthenticationPrincipal User user
    ) {
        UserResponseDto response = userService.getCurrentUser(user);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponseDto> updateAccount(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid UserUpdateRequestDto request
    ) {
        UserResponseDto response = userService.updateAccount(user, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid ChangePasswordRequestDto request
    ) {
        userService.changePassword(user, request);
        return ResponseEntity
                .noContent()
                .build();
    }

    @PatchMapping("/me/email")
    public ResponseEntity<UserResponseDto> changeEmail(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid ChangeEmailRequestDto request
    ) {
        UserResponseDto response = userService.changeEmail(user, request);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal User user
    ) {
        userService.deleteAccount(user);
        return ResponseEntity
                .noContent()
                .build();
    }
}
