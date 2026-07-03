package com.literandltx.timer.controller;

import com.literandltx.timer.config.env.JwtConfig;
import com.literandltx.timer.config.env.WebPropertiesConfig;
import com.literandltx.timer.dto.user.AuthTokensDto;
import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.dto.user.UserLoginResponseDto;
import com.literandltx.timer.dto.user.UserRegistrationRequestDto;
import com.literandltx.timer.dto.user.UserRegistrationResponseDto;
import com.literandltx.timer.security.AuthenticationService;
import com.literandltx.timer.service.UserService;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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
    private final JwtConfig jwtConfig;
    private final WebPropertiesConfig webPropertiesConfig;

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponseDto> register(
            @RequestBody @Valid UserRegistrationRequestDto request
    ) {
        UserRegistrationResponseDto response = userService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(
            @RequestBody @Valid UserLoginRequestDto request
    ) {
        AuthTokensDto tokens = authenticationService.login(request);

        ResponseCookie cookie = ResponseCookie.from("REFRESH_TOKEN", tokens.refreshToken())
                .httpOnly(true)
                .secure(webPropertiesConfig.cookie().secure())
                .path("/api/v1/auth")
                .sameSite(webPropertiesConfig.cookie().sameSite())
                .maxAge(Duration.ofMillis(jwtConfig.refresh().expiration()))
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new UserLoginResponseDto(tokens.accessToken()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserLoginResponseDto> refresh(
            @CookieValue(name = "REFRESH_TOKEN") String refreshToken
    ) {
        UserLoginResponseDto response = authenticationService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "REFRESH_TOKEN", required = false) String refreshToken
    ) {
        authenticationService.logout(refreshToken);

        ResponseCookie cookie = ResponseCookie.from("REFRESH_TOKEN", "")
                .httpOnly(true)
                .secure(webPropertiesConfig.cookie().secure())
                .path("/api/v1/auth")
                .sameSite(webPropertiesConfig.cookie().sameSite())
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

}
