package com.literandltx.timer.config.env;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @Valid @NotNull Init init,
        @Valid @NotNull RateLimit rateLimit
) {
    public record Init(
            @NotBlank @Email String adminEmail,
            @NotBlank String defaultAdminPassword
    ) {
    }

    public record RateLimit(
            @Min(1) int cacheMaxSize,
            @NotNull Duration cacheExpire,
            @Min(1) int bucketCapacity,
            @NotNull Duration refillInterval
    ) {
    }

}
