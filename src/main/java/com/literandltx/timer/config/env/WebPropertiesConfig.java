package com.literandltx.timer.config.env;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "web")
@Validated
public record WebPropertiesConfig(
        @NotNull Cors cors,
        @NotNull Cookie cookie
) {
    public record Cors(
            @NotEmpty List<String> allowedOrigins
    ) {}

    public record Cookie(
            boolean secure,
            @NotBlank String sameSite
    ) {}
}
