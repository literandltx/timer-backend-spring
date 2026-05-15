package com.example.timer_backend.controller;

import com.example.timer_backend.dto.user.UserLoginRequestDto;
import com.example.timer_backend.dto.user.UserLoginResponseDto;
import com.example.timer_backend.dto.user.UserRegistrationRequestDto;
import com.example.timer_backend.dto.user.UserRegistrationResponseDto;
import com.example.timer_backend.security.AuthenticationService;
import com.example.timer_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
    private final UserService userService;
    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public UserLoginResponseDto login(@RequestBody @Valid UserLoginRequestDto request) {
        return authenticationService.authenticate(request);
    }

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponseDto> register(@RequestBody @Valid UserRegistrationRequestDto request) {
        UserRegistrationResponseDto response = userService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}
