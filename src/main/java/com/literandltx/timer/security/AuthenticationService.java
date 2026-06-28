package com.literandltx.timer.security;

import com.literandltx.timer.config.env.JwtConfig;
import com.literandltx.timer.dto.user.AuthTokensDto;
import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.dto.user.UserLoginResponseDto;
import com.literandltx.timer.exception.custom.InvalidTokenTypeException;
import com.literandltx.timer.exception.custom.TokenRefreshException;
import com.literandltx.timer.model.RefreshToken;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.RefreshTokenRepository;
import com.literandltx.timer.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@RequiredArgsConstructor
@Service
public class AuthenticationService {

    public static final int MAX_ACTIVE_DEVICES = 3;

    private final JwtConfig jwtConfig;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public AuthTokensDto login(UserLoginRequestDto requestDto) {
        log.info("Attempting authentication for user: {}", requestDto.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        requestDto.getUsername(),
                        requestDto.getPassword()
                )
        );

        String username = authentication.getName();
        log.info("User successfully authenticated: {}", username);

        User user = userRepository.findByEmailForAuth(username)
                .orElseThrow(() -> {
                    log.error("Authenticated user not found in database: {}", username);
                    return new RuntimeException("User not found");
                });

        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserOrderByIdAsc(user);

        if (activeTokens.size() >= MAX_ACTIVE_DEVICES) {
            int excessCount = activeTokens.size() - (MAX_ACTIVE_DEVICES - 1);
            log.info("User {} reached max active devices. Revoking {} oldest session(s).", username, excessCount);

            List<RefreshToken> tokensToDelete = activeTokens.subList(0, excessCount);
            refreshTokenRepository.deleteAll(tokensToDelete);
        }

        String accessToken = jwtUtil.generateAccessToken(username);
        String refreshTokenString = jwtUtil.generateToken(
                username,
                jwtConfig.refresh().expiration(),
                TokenType.REFRESH
        );

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenString)
                .expiryDate(Instant.now().plusMillis(jwtConfig.refresh().expiration()))
                .build();

        refreshTokenRepository.save(refreshToken);
        log.info("Generated new access and refresh tokens for user: {}", username);

        return new AuthTokensDto(accessToken, refreshToken.getToken());
    }

    @Transactional
    public UserLoginResponseDto refreshAccessToken(String requestRefreshToken) {
        log.info("Processing request to refresh access token");

        if (jwtUtil.getTokenType(requestRefreshToken) != TokenType.REFRESH) {
            log.warn("Invalid token type provided during refresh attempt");
            throw new InvalidTokenTypeException("Provided token is not a refresh token.");
        }

        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> {
                    log.warn("Refresh token rejected: not found in database");
                    return new TokenRefreshException("Refresh token not found in database");
                });

        if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            log.info("Refresh token expired for user: {}. Deleting from database.", refreshToken.getUser().getEmail());
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token has expired. Please sign in again.");
        }

        String newAccessToken = jwtUtil.generateAccessToken(refreshToken.getUser().getEmail());
        log.info("Successfully refreshed access token for user: {}", refreshToken.getUser().getEmail());

        return new UserLoginResponseDto(newAccessToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            log.warn("Logout attempted with missing or empty refresh token");
            return;
        }

        refreshTokenRepository.findByToken(refreshToken)
                .ifPresentOrElse(token -> {
                    refreshTokenRepository.delete(token);
                    log.info("Successfully logged out user: {}", token.getUser().getEmail());
                }, () -> log.info("Logout requested for token that does not exist in database"));
    }

}
