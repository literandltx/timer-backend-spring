package com.literandltx.timer.config.env;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtConfig(
        @NotBlank String secret,
        @Positive long expiration,
        @Valid @NotNull RefreshConfig refresh
) {
    public record RefreshConfig(
            @Positive long expiration
    ) {
    }
}
