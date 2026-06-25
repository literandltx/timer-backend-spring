package com.literandltx.timer.controller;

import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.dto.user.UserLoginResponseDto;
import com.literandltx.timer.dto.user.UserRegistrationRequestDto;
import com.literandltx.timer.dto.user.UserRegistrationResponseDto;
import com.literandltx.timer.security.AuthenticationService;
import com.literandltx.timer.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
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
    public ResponseEntity<UserLoginResponseDto> login(@RequestBody @Valid UserLoginRequestDto request, HttpServletResponse response) {
        String token = authenticationService.authenticate(request);

        ResponseCookie cookie = ResponseCookie.from("AUTH_TOKEN", token)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .sameSite("Strict")
                .maxAge(Duration.ofMinutes(90))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        UserLoginResponseDto responseBody = new UserLoginResponseDto(token);

        return ResponseEntity.ok(responseBody);
    }

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponseDto> register(@RequestBody @Valid UserRegistrationRequestDto request) {
        UserRegistrationResponseDto response = userService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

}
