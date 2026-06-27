package com.literandltx.timer.security;

import com.literandltx.timer.config.env.JwtConfig;
import com.literandltx.timer.dto.user.AuthTokensDto;
import com.literandltx.timer.dto.user.UserLoginRequestDto;
import com.literandltx.timer.dto.user.UserLoginResponseDto;
import com.literandltx.timer.exception.custom.TokenRefreshException;
import com.literandltx.timer.model.RefreshToken;
import com.literandltx.timer.model.User;
import com.literandltx.timer.repository.RefreshTokenRepository;
import com.literandltx.timer.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AuthenticationService {

    private final JwtConfig jwtConfig;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Transactional
    public AuthTokensDto login(UserLoginRequestDto requestDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        requestDto.getUsername(),
                        requestDto.getPassword()
                )
        );

        String username = authentication.getName();
        User user = userRepository.findByEmailForAuth(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accessToken = jwtUtil.generateToken(username);
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(jwtConfig.refresh().expiration()))
                .build();

        refreshTokenRepository.save(refreshToken);

        return new AuthTokensDto(accessToken, refreshToken.getToken());
    }

    @Transactional
    public UserLoginResponseDto refreshAccessToken(String requestRefreshToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found in database"));

        if (refreshToken.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenRefreshException("Refresh token has expired. Please sign in again.");
        }

        String newAccessToken = jwtUtil.generateToken(refreshToken.getUser().getEmail());

        return new UserLoginResponseDto(newAccessToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
    }

}
