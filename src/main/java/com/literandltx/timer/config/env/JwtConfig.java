package com.literandltx.timer.config.env;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "jwt")
@Validated
public record JwtConfig(
        @NotBlank String secret,
        @Positive long expiration,
        @NotNull RefreshConfig refresh
) {
    public record RefreshConfig(
            @Positive long expiration
    ) {
    }
}
