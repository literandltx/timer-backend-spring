package com.literandltx.timer.config.env;

import jakarta.validation.constraints.Positive;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        @Positive int cacheMaxSize,
        @Positive Duration cacheExpire,
        @Positive int bucketCapacity,
        @Positive Duration refillInterval
) {
}
